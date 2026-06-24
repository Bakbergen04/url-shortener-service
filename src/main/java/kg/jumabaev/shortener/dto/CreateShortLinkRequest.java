package kg.jumabaev.shortener.dto;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import kg.jumabaev.shortener.util.ValidHttpUrl;

import java.time.Instant;

public record CreateShortLinkRequest(
        @ValidHttpUrl
        String originalUrl,

        @Size(min = 4, max = 32, message = "customAlias must contain between 4 and 32 characters")
        @Pattern(
                regexp = "^[A-Za-z0-9_-]+$",
                message = "customAlias may contain only Latin letters, digits, hyphens, and underscores"
        )
        String customAlias,

        @Size(max = 255, message = "title must not exceed 255 characters")
        String title,

        @Future(message = "expiresAt must be in the future")
        Instant expiresAt
) {
}
