package kg.jumabaev.shortener.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(String baseUrl) {

    public AppProperties {
        if (baseUrl == null || baseUrl.isBlank()) {
            throw new IllegalArgumentException("app.base-url must not be blank");
        }
        baseUrl = baseUrl.replaceAll("/+$", "");
    }
}
