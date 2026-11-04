package br.com.carteira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.carteira.model.AcaoAdmin;
import br.com.carteira.model.AtividadeAdmin;
import br.com.carteira.repository.AtividadeAdminRepository;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.assertj.core.api.Assertions.within;

class AtividadeAdminServiceTest {
    private AtividadeAdminRepository repository;
    private AtividadeAdminService service;

    @BeforeEach
    void setUp() {
        repository = mock(AtividadeAdminRepository.class);
        service = new AtividadeAdminService(repository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void limparContexto() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void deveRegistrarQuemFezAAlteracao() {
        SecurityContextHolder.getContext().setAuthentication(
                UsernamePasswordAuthenticationToken.authenticated("admin@carteira.test", null, List.of()));

        service.registrar(AcaoAdmin.ALTEROU_PAPEL, "ana@carteira.test", "Investidor para Administrador");

        AtividadeAdmin salva = capturar();
        assertThat(salva.getResponsavel()).isEqualTo("admin@carteira.test");
        assertThat(salva.getAcao()).isEqualTo(AcaoAdmin.ALTEROU_PAPEL);
        assertThat(salva.getConta()).isEqualTo("ana@carteira.test");
        assertThat(salva.getDetalhe()).isEqualTo("Investidor para Administrador");
        assertThat(salva.getDataHora()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void semUsuarioAutenticadoORegistroFicaComoSistema() {
        service.registrar(AcaoAdmin.CRIOU_CONTA, "ana@carteira.test", null);

        assertThat(capturar().getResponsavel()).isEqualTo("sistema");
    }

    @Test
    void deveListarAsAtividadesMaisRecentes() {
        List<AtividadeAdmin> recentes = List.of(new AtividadeAdmin("admin", AcaoAdmin.REDEFINIU_SENHA, "ana", null));
        when(repository.findTop20ByOrderByDataHoraDescIdDesc()).thenReturn(recentes);

        assertThat(service.recentes()).isSameAs(recentes);
    }

    private AtividadeAdmin capturar() {
        ArgumentCaptor<AtividadeAdmin> captor = ArgumentCaptor.forClass(AtividadeAdmin.class);
        verify(repository).save(captor.capture());
        return captor.getValue();
    }
}
