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

    @Test
    void deveRejeitarPrimeiroDigitoVerificadorIncorreto() {
        assertThat(CnpjValidator.isValid("02.332.886/0001-14")).isFalse();
    }

    @Test
    void deveRejeitarTamanhoDiferenteDeQuatorzeDigitos() {
        assertThat(CnpjValidator.isValid("0233288600010")).isFalse();
        assertThat(CnpjValidator.isValid("023328860001044")).isFalse();
    }
}
