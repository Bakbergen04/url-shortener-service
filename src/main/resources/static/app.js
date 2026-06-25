const createForm = document.querySelector('#create-form');
const statsForm = document.querySelector('#stats-form');
const linkResult = document.querySelector('#link-result');
const statsResult = document.querySelector('#stats-result');
const createMessage = document.querySelector('#create-message');
const statsMessage = document.querySelector('#stats-message');
const shortUrlElement = document.querySelector('#short-url');
const copyButton = document.querySelector('#copy-button');

createForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearMessage(createMessage);
    linkResult.hidden = true;

    if (!createForm.reportValidity()) {
        return;
    }

    const formData = new FormData(createForm);
    const expiresAt = formData.get('expiresAt');
    const request = {
        originalUrl: formData.get('originalUrl').trim(),
        customAlias: optionalValue(formData.get('customAlias')),
        title: optionalValue(formData.get('title')),
        expiresAt: expiresAt ? new Date(expiresAt).toISOString() : null
    };

    const submitButton = createForm.querySelector('button[type="submit"]');
    setLoading(submitButton, true, 'Creating…');

    try {
        const link = await requestJson('/api/v1/links', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(request)
        });
        renderCreatedLink(link);
        document.querySelector('#stats-code').value = link.shortCode;
        showMessage(createMessage, 'Short link created successfully.', 'success');
    } catch (error) {
        showMessage(createMessage, error.message, 'error');
    } finally {
        setLoading(submitButton, false);
    }
});

statsForm.addEventListener('submit', async (event) => {
    event.preventDefault();
    clearMessage(statsMessage);
    statsResult.hidden = true;

    if (!statsForm.reportValidity()) {
        return;
    }

    const code = document.querySelector('#stats-code').value.trim();
    const submitButton = statsForm.querySelector('button[type="submit"]');
    setLoading(submitButton, true, 'Loading…');

    try {
        const stats = await requestJson(`/api/v1/links/${encodeURIComponent(code)}/stats`);
        renderStats(stats);
    } catch (error) {
        showMessage(statsMessage, error.message, 'error');
    } finally {
        setLoading(submitButton, false);
    }
});

copyButton.addEventListener('click', async () => {
    const value = shortUrlElement.textContent;
    try {
        await navigator.clipboard.writeText(value);
        copyButton.textContent = 'Copied';
        window.setTimeout(() => copyButton.textContent = 'Copy', 1600);
    } catch {
        showMessage(createMessage, 'Copy failed. Select the link and copy it manually.', 'error');
    }
});

async function requestJson(url, options = {}) {
    const response = await fetch(url, options);
    const contentType = response.headers.get('content-type') || '';
    const body = contentType.includes('application/json') ? await response.json() : null;

    if (!response.ok) {
        throw new Error(body?.message || `Request failed with status ${response.status}`);
    }
    return body;
}

function renderCreatedLink(link) {
    shortUrlElement.textContent = link.shortUrl;
    shortUrlElement.href = link.shortUrl;
    document.querySelector('#result-code').textContent = link.shortCode;
    document.querySelector('#result-clicks').textContent = link.clickCount;
    linkResult.hidden = false;
}

function renderStats(stats) {
    document.querySelector('#stats-clicks').textContent = stats.clickCount;
    document.querySelector('#stats-short-code').textContent = stats.shortCode;
    document.querySelector('#stats-last-accessed').textContent = stats.lastAccessedAt
        ? new Date(stats.lastAccessedAt).toLocaleString()
        : 'Never';

    const dailyClicks = document.querySelector('#daily-clicks');
    dailyClicks.replaceChildren();

    if (stats.dailyClicks.length === 0) {
        const empty = document.createElement('p');
        empty.className = 'empty-state';
        empty.textContent = 'No redirects recorded yet.';
        dailyClicks.append(empty);
    } else {
        const maximum = Math.max(...stats.dailyClicks.map(item => item.clicks));
        stats.dailyClicks.forEach(item => {
            const row = document.createElement('div');
            row.className = 'activity-row';

            const date = document.createElement('span');
            date.textContent = item.date;

            const track = document.createElement('div');
            track.className = 'activity-track';
            const bar = document.createElement('i');
            bar.style.width = `${Math.max(8, (item.clicks / maximum) * 100)}%`;
            track.append(bar);

            const count = document.createElement('strong');
            count.textContent = item.clicks;
            row.append(date, track, count);
            dailyClicks.append(row);
        });
    }
    statsResult.hidden = false;
}

function optionalValue(value) {
    const normalized = value.trim();
    return normalized.length > 0 ? normalized : null;
}

function setLoading(button, loading, loadingText) {
    if (loading) {
        button.dataset.label = button.textContent;
        button.textContent = loadingText;
        button.disabled = true;
    } else {
        button.textContent = button.dataset.label || button.textContent;
        button.disabled = false;
    }
}

function showMessage(element, message, type) {
    element.textContent = message;
    element.className = `message ${type}`;
}

function clearMessage(element) {
    element.textContent = '';
    element.className = 'message';
}
