package kg.bakbergen.shortener.dto;

import java.time.Instant;
import java.util.UUID;

public record ShortLinkResponse(
        UUID id,
        String originalUrl,
        String shortCode,
        String shortUrl,
        String title,
        boolean active,
        long clickCount,
        Instant createdAt,
        Instant expiresAt
) {
}
