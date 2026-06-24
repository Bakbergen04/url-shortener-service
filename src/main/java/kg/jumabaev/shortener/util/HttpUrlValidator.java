package kg.jumabaev.shortener.util;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

public class HttpUrlValidator implements ConstraintValidator<ValidHttpUrl, String> {

    private static final int MAX_URL_LENGTH = 2048;
    private static final Set<String> ALLOWED_SCHEMES = Set.of("http", "https");

    private boolean allowNull;

    @Override
    public void initialize(ValidHttpUrl constraintAnnotation) {
        allowNull = constraintAnnotation.allowNull();
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return allowNull;
        }
        if (value.isBlank() || value.length() > MAX_URL_LENGTH) {
            return false;
        }

        try {
            URI uri = new URI(value);
            return uri.isAbsolute()
                    && ALLOWED_SCHEMES.contains(uri.getScheme().toLowerCase())
                    && uri.getHost() != null
                    && !uri.getHost().isBlank();
        } catch (URISyntaxException exception) {
            return false;
        }
    }
}
