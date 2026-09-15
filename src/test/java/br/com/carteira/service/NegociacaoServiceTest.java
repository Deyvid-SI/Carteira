package br.com.carteira.service;

import br.com.carteira.dto.RegistrarNegociacaoRequest;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.model.Acao;
import br.com.carteira.model.Negociacao;
import br.com.carteira.model.TipoNegociacao;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.NegociacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NegociacaoServiceTest {
    private NegociacaoRepository repository;
    private AcaoRepository acaoRepository;
    private UsuarioAtualService usuarioAtual;
    private NegociacaoService service;
    private Acao acao;

    @BeforeEach
    void setUp() {
        repository = mock(NegociacaoRepository.class);
        acaoRepository = mock(AcaoRepository.class);
        usuarioAtual = mock(UsuarioAtualService.class);
        when(usuarioAtual.id()).thenReturn(7L);
        service = new NegociacaoService(repository, acaoRepository, usuarioAtual);
        acao = new Acao();
        acao.setTicker("PETR4");
        acao.setMoeda("BRL");
        acao.setQuantidade(new BigDecimal("10.0000"));
        acao.setPrecoMedio(new BigDecimal("100.0000"));
        acao.setCotacaoAtual(new BigDecimal("120.0000"));
        when(acaoRepository.buscarParaNegociacao(1L, 7L)).thenReturn(Optional.of(acao));
        when(repository.save(any(Negociacao.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveUsarPrecoInformadoNaCompraSemTaxaManual() {
        Negociacao negociacao = service.registrar(request(TipoNegociacao.COMPRA, "5", "130"));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("15.0000");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("110.0000");
        assertThat(negociacao.getPrecoUnitario()).isEqualByComparingTo("130.0000");
        assertThat(negociacao.getValorBruto()).isEqualByComparingTo("650.0000");
        assertThat(negociacao.getTaxas()).isEqualByComparingTo("0.0000");
        assertThat(negociacao.getValorLiquido()).isEqualByComparingTo("650.0000");
        assertThat(negociacao.getResultadoRealizado()).isEqualByComparingTo("0.0000");
    }

    @Test
    void deveManterPrecoMedioNaVendaMesmoComPrecoExecutadoDiferente() {
        Negociacao negociacao = service.registrar(request(TipoNegociacao.VENDA, "4", "130"));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("6.0000");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("100.0000");
        assertThat(negociacao.getPrecoUnitario()).isEqualByComparingTo("130.0000");
        assertThat(negociacao.getTaxas()).isEqualByComparingTo("0.0000");
        assertThat(negociacao.getValorLiquido()).isEqualByComparingTo("520.0000");
        assertThat(negociacao.getResultadoRealizado()).isEqualByComparingTo("120.0000");
    }

    @Test
    void deveManterPrecoMedioAposVendasComPrecosDiferentes() {
        Negociacao vendaComPrejuizo = service.registrar(request(TipoNegociacao.VENDA, "2", "70"));
        Negociacao vendaComLucro = service.registrar(request(TipoNegociacao.VENDA, "4", "160"));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("4.0000");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("100.0000");
        assertThat(vendaComPrejuizo.getResultadoRealizado()).isEqualByComparingTo("-60.0000");
        assertThat(vendaComLucro.getResultadoRealizado()).isEqualByComparingTo("240.0000");
    }

    @Test
    void deveRecusarVendaAcimaDoSaldoSemPersistir() {
        assertThatThrownBy(() -> service.registrar(request(TipoNegociacao.VENDA, "11", "120")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("supera o saldo disponível");

        verify(repository, never()).save(any());
        verify(acaoRepository, never()).save(any());
    }

    @Test
    void deveRecusarPrecoInformadoIgualAZero() {
        assertThatThrownBy(() -> service.registrar(request(TipoNegociacao.COMPRA, "1", "0")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("preço");

        verify(acaoRepository, never()).buscarParaNegociacao(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void deveRecusarQuantidadeFracionada() {
        assertThatThrownBy(() -> service.registrar(request(TipoNegociacao.COMPRA, "1.5", "100")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("número inteiro");

        verify(acaoRepository, never()).buscarParaNegociacao(any(), any());
    }

    private RegistrarNegociacaoRequest request(TipoNegociacao tipo, String quantidade, String precoUnitario) {
        RegistrarNegociacaoRequest request = new RegistrarNegociacaoRequest();
        request.setAcaoId(1L);
        request.setTipo(tipo);
        request.setQuantidade(new BigDecimal(quantidade));
        request.setPrecoUnitario(new BigDecimal(precoUnitario));
        request.setDataNegociacao(LocalDateTime.now().minusMinutes(1));
        return request;
    }
}
