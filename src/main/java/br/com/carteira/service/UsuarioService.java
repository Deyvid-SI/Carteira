package br.com.carteira.service;

import br.com.carteira.dto.CadastroUsuarioRequest;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.model.Usuario;
import br.com.carteira.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void cadastrar(CadastroUsuarioRequest request) {
        String email = request.getEmail().trim().toLowerCase(Locale.ROOT);
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException("Já existe uma conta com este e-mail.");
        }
        if (!request.getSenha().equals(request.getConfirmarSenha())) {
            throw new RegraNegocioException("A confirmação da senha não confere.");
        }
        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome().trim());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(request.getSenha()));
        repository.save(usuario);
    }
}
