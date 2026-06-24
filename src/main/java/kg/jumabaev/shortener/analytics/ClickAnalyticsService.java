package kg.jumabaev.shortener.analytics;

import kg.jumabaev.shortener.entity.ClickEvent;
import kg.jumabaev.shortener.entity.ShortLink;
import kg.jumabaev.shortener.repository.ClickEventRepository;
import kg.jumabaev.shortener.repository.ShortLinkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClickAnalyticsService {

    private static final int MAX_USER_AGENT_LENGTH = 1024;
    private static final int MAX_REFERER_LENGTH = 2048;

    private final ShortLinkRepository shortLinkRepository;
    private final ClickEventRepository clickEventRepository;

    @Transactional
    public void recordClick(UUID shortLinkId, String userAgent, String referer) {
        shortLinkRepository.incrementClickCount(shortLinkId);

        ShortLink shortLinkReference = shortLinkRepository.getReferenceById(shortLinkId);
        ClickEvent clickEvent = ClickEvent.builder()
                .shortLink(shortLinkReference)
                .userAgent(truncate(userAgent, MAX_USER_AGENT_LENGTH))
                .referer(truncate(referer, MAX_REFERER_LENGTH))
                .build();
        clickEventRepository.save(clickEvent);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
