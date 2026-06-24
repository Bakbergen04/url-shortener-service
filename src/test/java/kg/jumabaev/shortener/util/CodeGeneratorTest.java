package kg.jumabaev.shortener.util;

import org.junit.jupiter.api.Test;

import java.security.SecureRandom;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CodeGeneratorTest {

    @Test
    void generatesSevenCharacterBase62Code() {
        SecureRandom secureRandom = mock(SecureRandom.class);
        when(secureRandom.nextInt(CodeGenerator.BASE62_ALPHABET.length()))
                .thenReturn(0, 25, 26, 51, 52, 61, 1);

        String code = new CodeGenerator(secureRandom).generate();

        assertThat(code).isEqualTo("azAZ09b");
        assertThat(code).hasSize(CodeGenerator.DEFAULT_CODE_LENGTH);
    }

    @Test
    void generatedCodeContainsOnlyBase62Characters() {
        CodeGenerator generator = new CodeGenerator();

        String code = generator.generate();

        assertThat(code).matches("[a-zA-Z0-9]{7}");
    }
}
