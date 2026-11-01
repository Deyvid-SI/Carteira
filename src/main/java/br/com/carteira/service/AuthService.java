package br.com.carteira.service;

import br.com.carteira.config.JwtProperties;
import br.com.carteira.dto.LoginRequest;
import br.com.carteira.dto.TokenResponse;
import java.time.Instant;
import java.util.List;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AuthService {
    private final AuthenticationManager authenticationManager;
    private final JwtEncoder jwtEncoder;
    private final JwtProperties jwtProperties;

    public AuthService(AuthenticationManager authenticationManager, JwtEncoder jwtEncoder,
                       JwtProperties jwtProperties) {
        this.authenticationManager = authenticationManager;
        this.jwtEncoder = jwtEncoder;
        this.jwtProperties = jwtProperties;
    }

    /** Emite o JWT somente depois que o AuthenticationManager confere e-mail, senha BCrypt e conta ativa. */
    public TokenResponse login(LoginRequest request) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email().trim(), request.senha()));

        Instant emitidoEm = Instant.now();
        List<String> authorities = authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtProperties.issuer())
                .issuedAt(emitidoEm)
                .expiresAt(emitidoEm.plus(jwtProperties.expiration()))
                .subject(authentication.getName())
                .claim("authorities", authorities)
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", jwtProperties.expiration().toSeconds());
    }
}
