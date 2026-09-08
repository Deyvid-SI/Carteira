package br.com.carteira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import br.com.carteira.model.Acao;
import br.com.carteira.model.Mercado;
import br.com.carteira.model.Negociacao;
import br.com.carteira.model.TipoNegociacao;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.CorretoraRepository;
import br.com.carteira.repository.HistoricoCotacaoRepository;
import br.com.carteira.repository.NegociacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceTest {

    @Test
    void deveConsolidarValoresSeparadosPorMoeda() {
        Acao br = acao("PETR4", "BRL", Mercado.BRASIL, "10", "20", "25");
        Acao us = acao("IBM", "USD", Mercado.ESTADOS_UNIDOS, "2", "100", "110");
        AcaoRepository acoes = mock(AcaoRepository.class);
        CorretoraRepository corretoras = mock(CorretoraRepository.class);
        HistoricoCotacaoRepository historicos = mock(HistoricoCotacaoRepository.class);
        NegociacaoRepository negociacoes = mock(NegociacaoRepository.class);
        when(acoes.findAll()).thenReturn(List.of(br, us));
        when(corretoras.count()).thenReturn(1L);
        when(historicos.findTop30ByAcaoIdOrderByDataHoraDesc(any())).thenReturn(List.of());
        when(negociacoes.findAllByOrderByDataNegociacaoAscIdAsc()).thenReturn(List.of());

        var dashboard = new DashboardService(acoes, corretoras, historicos, negociacoes).dados();

        assertThat(dashboard.totalAtivos()).isEqualTo(2);
        assertThat(dashboard.resumosPorMoeda()).filteredOn(r -> r.moeda().equals("BRL")).singleElement()
                .satisfies(r -> {
                    assertThat(r.valorInvestido()).isEqualByComparingTo("200");
                    assertThat(r.valorAtual()).isEqualByComparingTo("250");
                    assertThat(r.resultado()).isEqualByComparingTo("50");
                });
        assertThat(dashboard.resumosPorMoeda()).filteredOn(r -> r.moeda().equals("USD")).singleElement()
                .satisfies(r -> assertThat(r.valorAtual()).isEqualByComparingTo("220"));
    }

    @Test
    void deveMontarSerieDePrecoMedioAcumuladoSemTaxaManual() {
        Acao acao = acao("PETR4", "BRL", Mercado.BRASIL, "20", "110", "120");
        Negociacao compra = new Negociacao();
        compra.setAcao(acao);
        compra.setTipo(TipoNegociacao.COMPRA);
        compra.setQuantidade(new BigDecimal("10"));
        compra.setPrecoUnitario(new BigDecimal("120"));
        compra.setTaxas(BigDecimal.ZERO);
        compra.setValorBruto(new BigDecimal("1200"));
        compra.setValorLiquido(new BigDecimal("1200"));
        compra.setResultadoRealizado(BigDecimal.ZERO);
        compra.setDataNegociacao(LocalDateTime.of(2026, 9, 1, 10, 0));

        AcaoRepository acoes = mock(AcaoRepository.class);
        CorretoraRepository corretoras = mock(CorretoraRepository.class);
        HistoricoCotacaoRepository historicos = mock(HistoricoCotacaoRepository.class);
        NegociacaoRepository negociacoes = mock(NegociacaoRepository.class);
        when(acoes.findAll()).thenReturn(List.of(acao));
        when(historicos.findTop30ByAcaoIdOrderByDataHoraDesc(any())).thenReturn(List.of());
        when(negociacoes.findAllByOrderByDataNegociacaoAscIdAsc()).thenReturn(List.of(compra));

        var dashboard = new DashboardService(acoes, corretoras, historicos, negociacoes).dados();

        assertThat(dashboard.precosMediosAcumulados()).singleElement().satisfies(serie -> {
            assertThat(serie.ticker()).isEqualTo("PETR4");
            assertThat(serie.labels()).containsExactly("Inicial", "01/09 10:00");
            assertThat(serie.valores()).containsExactly(new BigDecimal("100.0000"), new BigDecimal("110.0000"));
        });
    }

    private Acao acao(String ticker, String moeda, Mercado mercado, String quantidade,
                      String precoMedio, String cotacao) {
        Acao acao = new Acao();
        acao.setTicker(ticker); acao.setNomeEmpresa(ticker); acao.setMoeda(moeda); acao.setMercado(mercado);
        acao.setQuantidade(new BigDecimal(quantidade)); acao.setPrecoMedio(new BigDecimal(precoMedio));
        acao.setCotacaoAtual(new BigDecimal(cotacao)); acao.setVariacaoPercentual(BigDecimal.ZERO);
        acao.setDataHoraCotacao(LocalDateTime.now());
        return acao;
    }
}
