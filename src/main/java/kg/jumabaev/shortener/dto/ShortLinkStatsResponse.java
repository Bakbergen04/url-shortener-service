package kg.jumabaev.shortener.dto;

import java.time.Instant;
import java.util.List;

public record ShortLinkStatsResponse(
        String shortCode,
        String originalUrl,
        long clickCount,
        Instant createdAt,
        Instant lastAccessedAt,
        List<DailyClickResponse> dailyClicks
) {
}
