package br.com.carteira.service;

import br.com.carteira.dto.AlterarSenhaRequest;
import br.com.carteira.dto.CadastroUsuarioRequest;
import br.com.carteira.dto.UsuarioCreateRequest;
import br.com.carteira.dto.UsuarioUpdateRequest;
import br.com.carteira.exception.RecursoNaoEncontradoException;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.model.Usuario;
import br.com.carteira.repository.UsuarioRepository;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioService {
    private static final String EMAIL_DUPLICADO = "Já existe uma conta com este e-mail.";

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void cadastrar(CadastroUsuarioRequest request) {
        String email = normalizarEmail(request.getEmail());
        if (repository.existsByEmailIgnoreCase(email)) {
            throw new RegraNegocioException(EMAIL_DUPLICADO);
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

    @Transactional(readOnly = true)
    public Page<Usuario> listar(Pageable pageable) {
        return repository.findAll(pageable);
    }

    @Transactional(readOnly = true)
    public Usuario buscar(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    @Transactional
    public Usuario criar(UsuarioCreateRequest request) {
        String email = normalizarEmail(request.email());
        validarEmailDisponivel(email, null);

        Usuario usuario = new Usuario();
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        usuario.setPapel(request.papel());
        return repository.save(usuario);
    }

    /** Desativar ({@code ativo=false}) preserva o registro para auditoria e impede novos logins. */
    @Transactional
    public Usuario atualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = buscar(id);
        String email = normalizarEmail(request.email());
        validarEmailDisponivel(email, id);

        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setPapel(request.papel());
        usuario.setAtivo(request.ativo());
        return repository.save(usuario);
    }

    @Transactional
    public void alterarSenha(Long id, AlterarSenhaRequest request) {
        Usuario usuario = buscar(id);
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        repository.save(usuario);
    }

    private void validarEmailDisponivel(String email, Long idAtual) {
        repository.findByEmailIgnoreCase(email).ifPresent(existente -> {
            if (!existente.getId().equals(idAtual)) {
                throw new RegraNegocioException(EMAIL_DUPLICADO);
            }
        });
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
