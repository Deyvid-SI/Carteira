package br.com.carteira.config;

import br.com.carteira.model.Usuario;
import br.com.carteira.repository.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class DataInitializer implements CommandLineRunner {
    private final UsuarioRepository repository;
    private final PasswordEncoder encoder;
    private final boolean enabled;
    private final String email;
    private final String password;

    public DataInitializer(UsuarioRepository repository, PasswordEncoder encoder,
                           @Value("${app.demo-user.enabled:false}") boolean enabled,
                           @Value("${app.demo-user.email:admin@carteira.com}") String email,
                           @Value("${app.demo-user.password:}") String password) {
        this.repository = repository;
        this.encoder = encoder;
        this.enabled = enabled;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(String... args) {
        if (enabled && !StringUtils.hasText(password)) {
            throw new IllegalStateException(
                    "DEMO_USER_PASSWORD deve ser informada quando DEMO_USER_ENABLED=true");
        }
        if (enabled && !repository.existsByEmailIgnoreCase(email)) {
            Usuario usuario = new Usuario();
            usuario.setNome("Administrador da carteira");
            usuario.setEmail(email);
            usuario.setSenha(encoder.encode(password));
            usuario.setPapel("ADMIN");
            repository.save(usuario);
        }
    }
}
