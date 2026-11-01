package br.com.carteira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.carteira.model.Usuario;
import br.com.carteira.repository.UsuarioRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

class UsuarioAtualServiceTest {
    private UsuarioRepository repository;
    private UsuarioAtualService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        service = new UsuarioAtualService(repository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveDevolverAContaDoUsuarioAutenticado() {
        Usuario usuario = new Usuario();
        ReflectionTestUtils.setField(usuario, "id", 7L);
        when(repository.findByEmailIgnoreCase("ana@carteira.test")).thenReturn(Optional.of(usuario));
        autenticar(UsernamePasswordAuthenticationToken.authenticated("ana@carteira.test", null,
                List.of(new SimpleGrantedAuthority("ROLE_USER"))));

        assertThat(service.obter()).isSameAs(usuario);
        assertThat(service.id()).isEqualTo(7L);
    }

    @Test
    void deveNegarQuandoNaoHouverAutenticacao() {
        assertThatThrownBy(service::obter)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("É necessário entrar para acessar a carteira.");
    }

    @Test
    void deveNegarAutenticacaoNaoConcluida() {
        autenticar(UsernamePasswordAuthenticationToken.unauthenticated("ana@carteira.test", "senha"));

        assertThatThrownBy(service::obter).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void deveNegarVisitanteAnonimo() {
        autenticar(new AnonymousAuthenticationToken("chave", "anonymousUser",
                List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))));

        assertThatThrownBy(service::obter)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("É necessário entrar para acessar a carteira.");
    }

    @Test
    void deveNegarTokenDeContaQueNaoExisteMais() {
        when(repository.findByEmailIgnoreCase("removida@carteira.test")).thenReturn(Optional.empty());
        autenticar(UsernamePasswordAuthenticationToken.authenticated("removida@carteira.test", null, List.of()));

        assertThatThrownBy(service::obter)
                .isInstanceOf(AccessDeniedException.class)
                .hasMessage("A conta autenticada não foi encontrada.");
    }

    private static void autenticar(Authentication authentication) {
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
