package kg.jumabaev.shortener.util;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class HttpUrlValidatorTest {

    private static Validator validator;

    @BeforeAll
    static void setUpValidator() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void acceptsHttpAndHttpsUrls() {
        assertThat(validator.validate(new UrlHolder("http://example.com/path"))).isEmpty();
        assertThat(validator.validate(new UrlHolder("https://example.com/path?q=value"))).isEmpty();
    }

    @Test
    void rejectsUnsupportedOrDangerousSchemes() {
        assertThat(validator.validate(new UrlHolder("javascript:alert(1)"))).isNotEmpty();
        assertThat(validator.validate(new UrlHolder("file:///etc/passwd"))).isNotEmpty();
        assertThat(validator.validate(new UrlHolder("ftp://example.com/file"))).isNotEmpty();
    }

    @Test
    void rejectsNullBlankRelativeAndOversizedUrls() {
        assertThat(validator.validate(new UrlHolder(null))).isNotEmpty();
        assertThat(validator.validate(new UrlHolder("  "))).isNotEmpty();
        assertThat(validator.validate(new UrlHolder("/relative/path"))).isNotEmpty();
        assertThat(validator.validate(new UrlHolder("https://example.com/" + "a".repeat(2049)))).isNotEmpty();
    }

    private record UrlHolder(@ValidHttpUrl String url) {
    }
}
