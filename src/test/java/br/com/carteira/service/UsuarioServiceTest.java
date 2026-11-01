package br.com.carteira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.carteira.dto.AlterarSenhaRequest;
import br.com.carteira.dto.CadastroUsuarioRequest;
import br.com.carteira.dto.UsuarioCreateRequest;
import br.com.carteira.dto.UsuarioUpdateRequest;
import br.com.carteira.exception.RecursoNaoEncontradoException;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.model.Papel;
import br.com.carteira.model.Usuario;
import br.com.carteira.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Lista de comportamentos da atividade guiada da Aula 15. O repositório e o encoder são dublês porque são
 * fronteiras (banco e BCrypt); a regra testada é a do próprio serviço.
 */
class UsuarioServiceTest {
    private UsuarioRepository repository;
    private PasswordEncoder passwordEncoder;
    private UsuarioService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        service = new UsuarioService(repository, passwordEncoder);
        when(repository.save(any(Usuario.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(passwordEncoder.encode(any())).thenAnswer(invocation -> "hash-de-" + invocation.getArgument(0));
    }

    @Test
    void deveCadastrarEmailDisponivelComSenhaCodificada() {
        when(repository.findByEmailIgnoreCase("ana@carteira.com")).thenReturn(Optional.empty());

        Usuario criado = service.criar(new UsuarioCreateRequest(" Ana ", " Ana@Carteira.com ", "SenhaForte123", Papel.USER));

        assertThat(criado.getNome()).isEqualTo("Ana");
        assertThat(criado.getEmail()).isEqualTo("ana@carteira.com");
        assertThat(criado.getSenha()).isEqualTo("hash-de-SenhaForte123");
        assertThat(criado.getPapel()).isEqualTo(Papel.USER);
        assertThat(criado.isAtivo()).isTrue();
    }

    @Test
    void deveRecusarEmailJaExistenteNoCadastroAdministrativo() {
        when(repository.findByEmailIgnoreCase("ana@carteira.com")).thenReturn(Optional.of(usuario(1L, "ana@carteira.com")));

        assertThatThrownBy(() -> service.criar(
                new UsuarioCreateRequest("Ana", "ana@carteira.com", "OutraSenha123", Papel.USER)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe uma conta com este e-mail.");
        verify(repository, never()).save(any());
    }

    @Test
    void atualizacaoPodeManterOProprioEmail() {
        Usuario existente = usuario(1L, "ana@carteira.com");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));
        when(repository.findByEmailIgnoreCase("ana@carteira.com")).thenReturn(Optional.of(existente));

        Usuario atualizado = service.atualizar(1L, new UsuarioUpdateRequest("Ana Souza", "ana@carteira.com", Papel.ADMIN, false));

        assertThat(atualizado.getNome()).isEqualTo("Ana Souza");
        assertThat(atualizado.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(atualizado.isAtivo()).isFalse();
    }

    @Test
    void atualizacaoNaoPodeUsarEmailDeOutroUsuario() {
        when(repository.findById(1L)).thenReturn(Optional.of(usuario(1L, "ana@carteira.com")));
        when(repository.findByEmailIgnoreCase("bia@carteira.com")).thenReturn(Optional.of(usuario(2L, "bia@carteira.com")));

        assertThatThrownBy(() -> service.atualizar(1L,
                new UsuarioUpdateRequest("Ana", "bia@carteira.com", Papel.USER, true)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("e-mail");
        verify(repository, never()).save(any());
    }

    @Test
    void deveInformarIdInexistente() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(99L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Usuário não encontrado.");
    }

    @Test
    void trocaDeSenhaGravaHashDiferenteDoTextoRecebido() {
        Usuario existente = usuario(1L, "ana@carteira.com");
        existente.setSenha("hash-antigo");
        when(repository.findById(1L)).thenReturn(Optional.of(existente));

        service.alterarSenha(1L, new AlterarSenhaRequest("NovaSenha456"));

        assertThat(existente.getSenha()).isEqualTo("hash-de-NovaSenha456").isNotEqualTo("NovaSenha456");
        verify(repository).save(existente);
    }

    @Test
    void cadastroWebCriaContaComPapelUser() {
        when(repository.existsByEmailIgnoreCase("bia@carteira.com")).thenReturn(false);

        service.cadastrar(cadastro("Bia@Carteira.com", "SenhaForte123", "SenhaForte123"));

        verify(repository).save(argThat(u -> u.getEmail().equals("bia@carteira.com")
                && u.getPapel() == Papel.USER && u.getSenha().equals("hash-de-SenhaForte123")));
    }

    @Test
    void cadastroWebRecusaEmailDuplicado() {
        when(repository.existsByEmailIgnoreCase("bia@carteira.com")).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(cadastro("bia@carteira.com", "SenhaForte123", "SenhaForte123")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Já existe uma conta com este e-mail.");
    }

    @Test
    void cadastroWebRecusaConfirmacaoDiferente() {
        assertThatThrownBy(() -> service.cadastrar(cadastro("bia@carteira.com", "SenhaForte123", "SenhaForte124")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("confirmação");
        verify(repository, never()).save(any());
    }

    private static Usuario usuario(Long id, String email) {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", id);
        usuario.setNome("Conta");
        usuario.setEmail(email);
        return usuario;
    }

    private static CadastroUsuarioRequest cadastro(String email, String senha, String confirmacao) {
        CadastroUsuarioRequest request = new CadastroUsuarioRequest();
        request.setNome("Bia");
        request.setEmail(email);
        request.setSenha(senha);
        request.setConfirmarSenha(confirmacao);
        return request;
    }
}
