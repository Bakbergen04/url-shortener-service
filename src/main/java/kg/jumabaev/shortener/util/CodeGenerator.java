package kg.jumabaev.shortener.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class CodeGenerator {

    static final String BASE62_ALPHABET = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    static final int DEFAULT_CODE_LENGTH = 7;

    private final SecureRandom secureRandom;

    public CodeGenerator() {
        this(new SecureRandom());
    }

    CodeGenerator(SecureRandom secureRandom) {
        this.secureRandom = secureRandom;
    }

    public String generate() {
        StringBuilder code = new StringBuilder(DEFAULT_CODE_LENGTH);
        for (int index = 0; index < DEFAULT_CODE_LENGTH; index++) {
            code.append(BASE62_ALPHABET.charAt(secureRandom.nextInt(BASE62_ALPHABET.length())));
        }
        return code.toString();
    }
}
