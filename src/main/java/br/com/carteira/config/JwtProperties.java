package br.com.carteira.config;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Configuração do JWT lida de {@code app.jwt.*}. A validação no construtor faz a aplicação
 * falhar na inicialização quando a chave, a validade ou o emissor estiverem ausentes ou fracos.
 */
@ConfigurationProperties(prefix = "app.jwt")
public record JwtProperties(String secret, Duration expiration, String issuer) {
    public static final int TAMANHO_MINIMO_CHAVE = 32;

    public JwtProperties {
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < TAMANHO_MINIMO_CHAVE) {
            throw new IllegalArgumentException(
                    "JWT_SECRET (app.jwt.secret) deve possuir pelo menos 32 bytes");
        }
        if (expiration == null || expiration.isNegative() || expiration.isZero()) {
            throw new IllegalArgumentException("JWT_EXPIRATION (app.jwt.expiration) deve ser maior que zero");
        }
        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException("JWT_ISSUER (app.jwt.issuer) deve ser informado");
        }
    }
}
