package kg.bakbergen.shortener.service;

import kg.bakbergen.shortener.analytics.ClickAnalyticsService;
import kg.bakbergen.shortener.cache.CachedShortLink;
import kg.bakbergen.shortener.cache.ShortLinkCacheService;
import kg.bakbergen.shortener.entity.ShortLink;
import kg.bakbergen.shortener.exception.GoneException;
import kg.bakbergen.shortener.exception.NotFoundException;
import kg.bakbergen.shortener.repository.ShortLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RedirectService {

    private static final Duration DEFAULT_CACHE_TTL = Duration.ofMinutes(10);

    private final ShortLinkRepository shortLinkRepository;
    private final ShortLinkCacheService cacheService;
    private final ClickAnalyticsService clickAnalyticsService;
    private final Clock clock;

    public String resolve(String shortCode, String userAgent, String referer) {
        return cacheService.get(shortCode)
                .map(cachedLink -> resolveFromCache(shortCode, cachedLink, userAgent, referer))
                .orElseGet(() -> resolveFromDatabase(shortCode, userAgent, referer));
    }

    private String resolveFromCache(
            String shortCode,
            CachedShortLink cachedLink,
            String userAgent,
            String referer
    ) {
        validateAvailable(shortCode, cachedLink.active(), cachedLink.expiresAt());
        UUID shortLinkId = shortLinkRepository.findIdByShortCode(shortCode)
                .orElseThrow(() -> new NotFoundException("Short link '%s' was not found".formatted(shortCode)));
        clickAnalyticsService.recordClick(shortLinkId, userAgent, referer);
        return cachedLink.originalUrl();
    }

    private String resolveFromDatabase(String shortCode, String userAgent, String referer) {
        ShortLink shortLink = shortLinkRepository.findByShortCode(shortCode)
                .orElseThrow(() -> new NotFoundException("Short link '%s' was not found".formatted(shortCode)));
        validateAvailable(shortCode, shortLink.isActive(), shortLink.getExpiresAt());

        clickAnalyticsService.recordClick(shortLink.getId(), userAgent, referer);
        cacheService.put(
                shortCode,
                new CachedShortLink(shortLink.getOriginalUrl(), shortLink.isActive(), shortLink.getExpiresAt()),
                calculateTtl(shortLink.getExpiresAt())
        );
        return shortLink.getOriginalUrl();
    }

    private void validateAvailable(String shortCode, boolean active, Instant expiresAt) {
        if (!active) {
            throw new GoneException("Short link '%s' is inactive".formatted(shortCode));
        }
        if (expiresAt != null && !expiresAt.isAfter(clock.instant())) {
            cacheService.evict(shortCode);
            throw new GoneException("Short link '%s' has expired".formatted(shortCode));
        }
    }

    private Duration calculateTtl(Instant expiresAt) {
        if (expiresAt == null) {
            return DEFAULT_CACHE_TTL;
        }
        Duration untilExpiration = Duration.between(clock.instant(), expiresAt);
        return untilExpiration.compareTo(DEFAULT_CACHE_TTL) < 0 ? untilExpiration : DEFAULT_CACHE_TTL;
    }
}
