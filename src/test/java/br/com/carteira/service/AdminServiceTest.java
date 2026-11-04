package br.com.carteira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.carteira.dto.PainelAdmin;
import br.com.carteira.model.Papel;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.CorretoraRepository;
import br.com.carteira.repository.NegociacaoRepository;
import br.com.carteira.repository.UsuarioRepository;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class AdminServiceTest {
    private UsuarioRepository usuarios;
    private AcaoRepository acoes;
    private CorretoraRepository corretoras;
    private NegociacaoRepository negociacoes;

    @BeforeEach
    void setUp() {
        usuarios = mock(UsuarioRepository.class);
        acoes = mock(AcaoRepository.class);
        corretoras = mock(CorretoraRepository.class);
        negociacoes = mock(NegociacaoRepository.class);
    }

    @Test
    void deveConsolidarTotaisDoSistema() {
        LocalDateTime ultimaCotacao = LocalDateTime.of(2026, 10, 20, 17, 0);
        when(usuarios.count()).thenReturn(10L);
        when(usuarios.countByAtivoTrue()).thenReturn(8L);
        when(usuarios.countByPapelAndAtivoTrue(Papel.ADMIN)).thenReturn(2L);
        when(usuarios.countByDataCadastroGreaterThanEqual(any())).thenReturn(3L);
        when(acoes.count()).thenReturn(25L);
        when(acoes.buscarUltimaCotacao()).thenReturn(ultimaCotacao);
        when(corretoras.count()).thenReturn(4L);
        when(negociacoes.count()).thenReturn(60L);

        PainelAdmin painel = servico("", "demo", true).painel();

        assertThat(painel).isEqualTo(new PainelAdmin(10, 8, 2, 2, 3, 25, 4, 60, ultimaCotacao, false, false, true));
    }

    @Test
    void sistemaSemAtivosNaoTemUltimaCotacao() {
        assertThat(servico("", "demo", false).painel().ultimaCotacao()).isNull();
    }

    @ParameterizedTest
    @CsvSource({
            "'',        demo,         false, false",
            "token-x,   demo,         true,  false",
            "'',        DEMO,         false, false",
            "'',        '  ',         false, false",
            "token-x,   chave-propria, true, true"})
    void deveIndicarIntegracoesSemExporAsChaves(String token, String chave, boolean brapi, boolean alpha) {
        PainelAdmin painel = servico(token, chave, false).painel();

        assertThat(painel.brapiComToken()).isEqualTo(brapi);
        assertThat(painel.alphaVantageComChavePropria()).isEqualTo(alpha);
        assertThat(painel.toString()).doesNotContain("token-x").doesNotContain("chave-propria");
    }

    private AdminService servico(String token, String chave, boolean h2) {
        return new AdminService(usuarios, acoes, corretoras, negociacoes, token, chave, h2);
    }
}
