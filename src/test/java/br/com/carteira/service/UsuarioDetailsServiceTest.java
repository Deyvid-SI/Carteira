package br.com.carteira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.carteira.model.Papel;
import br.com.carteira.model.Usuario;
import br.com.carteira.repository.UsuarioRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class UsuarioDetailsServiceTest {
    private UsuarioRepository repository;
    private UsuarioDetailsService service;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        service = new UsuarioDetailsService(repository);
    }

    @Test
    void contaAtivaRecebePapelComPrefixoRole() {
        when(repository.findByEmailIgnoreCase("Admin@Carteira.com")).thenReturn(Optional.of(usuario(Papel.ADMIN, true)));

        UserDetails detalhes = service.loadUserByUsername("Admin@Carteira.com");

        assertThat(detalhes.getUsername()).isEqualTo("admin@carteira.com");
        assertThat(detalhes.getPassword()).isEqualTo("$2a$hash");
        assertThat(detalhes.isEnabled()).isTrue();
        assertThat(detalhes.getAuthorities()).extracting(GrantedAuthority::getAuthority).containsExactly("ROLE_ADMIN");
    }

    @Test
    void contaDesativadaFicaDesabilitada() {
        when(repository.findByEmailIgnoreCase("admin@carteira.com")).thenReturn(Optional.of(usuario(Papel.USER, false)));

        assertThat(service.loadUserByUsername("admin@carteira.com").isEnabled()).isFalse();
    }

    @Test
    void emailInexistenteLancaUsernameNotFound() {
        when(repository.findByEmailIgnoreCase("ninguem@carteira.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ninguem@carteira.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    private static Usuario usuario(Papel papel, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setEmail("admin@carteira.com");
        usuario.setSenha("$2a$hash");
        usuario.setPapel(papel);
        usuario.setAtivo(ativo);
        return usuario;
    }
}
