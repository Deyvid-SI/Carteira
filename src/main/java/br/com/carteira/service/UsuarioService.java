package br.com.carteira.service;

import br.com.carteira.dto.AlterarSenhaRequest;
import br.com.carteira.dto.CadastroUsuarioRequest;
import br.com.carteira.dto.UsuarioCreateRequest;
import br.com.carteira.dto.UsuarioUpdateRequest;
import br.com.carteira.exception.RecursoNaoEncontradoException;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.model.AcaoAdmin;
import br.com.carteira.model.Papel;
import br.com.carteira.model.Usuario;
import br.com.carteira.repository.UsuarioRepository;
import java.util.List;
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
    private final AtividadeAdminService atividades;

    public UsuarioService(UsuarioRepository repository, PasswordEncoder passwordEncoder,
                          AtividadeAdminService atividades) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.atividades = atividades;
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
    public List<Usuario> listarTodos() {
        return repository.findAllByOrderByNomeAscEmailAsc();
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
        Usuario criado = repository.save(usuario);
        atividades.registrar(AcaoAdmin.CRIOU_CONTA, email, "Papel: " + request.papel().getDescricao());
        return criado;
    }

    /** Desativar ({@code ativo=false}) preserva o registro para auditoria e impede novos logins. */
    @Transactional
    public Usuario atualizar(Long id, UsuarioUpdateRequest request) {
        Usuario usuario = buscar(id);
        String email = normalizarEmail(request.email());
        validarEmailDisponivel(email, id);
        manterAdministradorAtivo(usuario, request.papel(), request.ativo());

        String detalhe = "Papel: " + request.papel().getDescricao() + ", " + (request.ativo() ? "ativa" : "desativada");
        usuario.setNome(request.nome().trim());
        usuario.setEmail(email);
        usuario.setPapel(request.papel());
        usuario.setAtivo(request.ativo());
        Usuario atualizado = repository.save(usuario);
        atividades.registrar(AcaoAdmin.ALTEROU_CADASTRO, email, detalhe);
        return atualizado;
    }

    @Transactional
    public Usuario definirPapel(Long id, Papel papel, String responsavel) {
        Usuario usuario = buscar(id);
        if (usuario.getPapel() == papel) return usuario;
        impedirAlteracaoDaPropriaConta(usuario, responsavel, "Você não pode alterar o papel da sua própria conta.");
        manterAdministradorAtivo(usuario, papel, usuario.isAtivo());

        String detalhe = usuario.getPapel().getDescricao() + " para " + papel.getDescricao();
        usuario.setPapel(papel);
        Usuario atualizado = repository.save(usuario);
        atividades.registrar(AcaoAdmin.ALTEROU_PAPEL, usuario.getEmail(), detalhe);
        return atualizado;
    }

    @Transactional
    public Usuario definirSituacao(Long id, boolean ativo, String responsavel) {
        Usuario usuario = buscar(id);
        if (usuario.isAtivo() == ativo) return usuario;
        impedirAlteracaoDaPropriaConta(usuario, responsavel, "Você não pode desativar a sua própria conta.");
        manterAdministradorAtivo(usuario, usuario.getPapel(), ativo);

        usuario.setAtivo(ativo);
        Usuario atualizado = repository.save(usuario);
        atividades.registrar(ativo ? AcaoAdmin.REATIVOU_CONTA : AcaoAdmin.DESATIVOU_CONTA, usuario.getEmail(), null);
        return atualizado;
    }

    @Transactional
    public Usuario alterarSenha(Long id, AlterarSenhaRequest request) {
        Usuario usuario = buscar(id);
        usuario.setSenha(passwordEncoder.encode(request.senha()));
        Usuario atualizado = repository.save(usuario);
        atividades.registrar(AcaoAdmin.REDEFINIU_SENHA, usuario.getEmail(), null);
        return atualizado;
    }

    private void validarEmailDisponivel(String email, Long idAtual) {
        repository.findByEmailIgnoreCase(email).ifPresent(existente -> {
            if (!existente.getId().equals(idAtual)) {
                throw new RegraNegocioException(EMAIL_DUPLICADO);
            }
        });
    }

    /** Sem administrador ativo, ninguém mais conseguiria gerenciar as contas pelo sistema. */
    private void manterAdministradorAtivo(Usuario usuario, Papel novoPapel, boolean novoAtivo) {
        boolean eraAdministradorAtivo = usuario.getPapel() == Papel.ADMIN && usuario.isAtivo();
        boolean continuaAdministradorAtivo = novoPapel == Papel.ADMIN && novoAtivo;
        if (eraAdministradorAtivo && !continuaAdministradorAtivo
                && repository.countByPapelAndAtivoTrue(Papel.ADMIN) <= 1) {
            throw new RegraNegocioException("O sistema precisa manter pelo menos um administrador ativo.");
        }
    }

    private static void impedirAlteracaoDaPropriaConta(Usuario usuario, String responsavel, String mensagem) {
        if (usuario.getEmail().equalsIgnoreCase(responsavel)) {
            throw new RegraNegocioException(mensagem);
        }
    }

    private static String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
