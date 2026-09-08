package br.com.carteira.util;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class CnpjValidatorTest {
    @Test
    void deveAceitarCnpjComDigitosValidos() {
        assertThat(CnpjValidator.isValid("02.332.886/0001-04")).isTrue();
    }

    @Test
    void deveRejeitarCnpjComDigitosInvalidosOuRepetidos() {
        assertThat(CnpjValidator.isValid("02.332.886/0001-05")).isFalse();
        assertThat(CnpjValidator.isValid("11.111.111/1111-11")).isFalse();
        assertThat(CnpjValidator.isValid(null)).isFalse();
    }
}
