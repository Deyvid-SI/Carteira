package br.com.carteira.controller.api;

import br.com.carteira.dto.LoginRequest;
import br.com.carteira.dto.TokenResponse;
import br.com.carteira.dto.UsuarioAutenticadoResponse;
import br.com.carteira.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Autenticação", description = "Login com e-mail e senha e emissão de JWT")
public class AuthRestController {
    private final AuthService service;

    public AuthRestController(AuthService service) { this.service = service; }

    @PostMapping("/login")
    @SecurityRequirements
    @Operation(summary = "Autenticar e receber um Bearer Token JWT")
    public TokenResponse login(@Valid @RequestBody LoginRequest request) {
        return service.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Identificar o usuário e as permissões representados pelo token")
    public UsuarioAutenticadoResponse me(Authentication authentication) {
        return new UsuarioAutenticadoResponse(authentication.getName(),
                authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());
    }
}
