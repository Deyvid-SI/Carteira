package br.com.carteira.config;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import br.com.carteira.model.Papel;
import br.com.carteira.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

class DataInitializerTest {
    private static final String EMAIL = "admin@carteira.com";

    private UsuarioRepository repository;
    private PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        repository = mock(UsuarioRepository.class);
        encoder = mock(PasswordEncoder.class);
        when(encoder.encode("SenhaDoAmbiente1")).thenReturn("$2a$hash");
    }

    @Test
    void criaAdministradorComSenhaCodificadaQuandoHabilitado() {
        when(repository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);

        new DataInitializer(repository, encoder, true, EMAIL, "SenhaDoAmbiente1").run();

        verify(repository).save(argThat(u -> u.getEmail().equals(EMAIL)
                && u.getPapel() == Papel.ADMIN && u.getSenha().equals("$2a$hash")));
    }

    @Test
    void naoRecriaAdministradorExistente() {
        when(repository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

        new DataInitializer(repository, encoder, true, EMAIL, "SenhaDoAmbiente1").run();

        verify(repository, never()).save(any());
    }

    @Test
    void exigeSenhaDoAmbienteQuandoHabilitado() {
        DataInitializer initializer = new DataInitializer(repository, encoder, true, EMAIL, "");

        assertThatThrownBy(initializer::run)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("DEMO_USER_PASSWORD");
    }

    @Test
    void naoFazNadaQuandoDesabilitado() {
        new DataInitializer(repository, encoder, false, EMAIL, "").run();

        verifyNoInteractions(repository, encoder);
    }
}
