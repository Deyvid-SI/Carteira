package br.com.carteira.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class JwtPropertiesTest {
    private static final Duration UMA_HORA = Duration.ofHours(1);

    @Test
    void chaveCom32BytesEhAceita() {
        assertThatCode(() -> new JwtProperties("a".repeat(32), UMA_HORA, "carteira-acao")).doesNotThrowAnyException();
    }

    @Test
    void chaveCom31BytesEhRecusada() {
        assertThatThrownBy(() -> new JwtProperties("a".repeat(31), UMA_HORA, "carteira-acao"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("32 bytes");
    }

    @Test
    void tamanhoDaChaveEhMedidoEmBytesUtf8() {
        // 16 caracteres "ç" ocupam 32 bytes em UTF-8.
        assertThatCode(() -> new JwtProperties("ç".repeat(16), UMA_HORA, "carteira-acao")).doesNotThrowAnyException();
    }

    @ParameterizedTest
    @NullAndEmptySource
    void chaveAusenteEhRecusada(String chave) {
        assertThatThrownBy(() -> new JwtProperties(chave, UMA_HORA, "carteira-acao"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(longs = {0, -1})
    void validadeDeveSerMaiorQueZero(long segundos) {
        assertThatThrownBy(() -> new JwtProperties("a".repeat(32), Duration.ofSeconds(segundos), "carteira-acao"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("maior que zero");
    }

    @Test
    void validadeAusenteEhRecusada() {
        assertThatThrownBy(() -> new JwtProperties("a".repeat(32), null, "carteira-acao"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    void emissorDeveSerInformado(String emissor) {
        assertThatThrownBy(() -> new JwtProperties("a".repeat(32), UMA_HORA, emissor))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("issuer");
    }
}
