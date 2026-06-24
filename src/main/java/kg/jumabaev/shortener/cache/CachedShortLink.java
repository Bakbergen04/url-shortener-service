package kg.jumabaev.shortener.cache;

import java.time.Instant;

public record CachedShortLink(
        String originalUrl,
        boolean active,
        Instant expiresAt
) {
}
