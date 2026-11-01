package br.com.carteira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import br.com.carteira.model.Acao;
import br.com.carteira.model.HistoricoCotacao;
import br.com.carteira.model.Mercado;
import br.com.carteira.model.Negociacao;
import br.com.carteira.model.TipoNegociacao;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.CorretoraRepository;
import br.com.carteira.repository.HistoricoCotacaoRepository;
import br.com.carteira.repository.NegociacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DashboardServiceTest {

    @Test
    void deveConsolidarValoresSeparadosPorMoeda() {
        Acao br = acao("PETR4", "BRL", Mercado.BRASIL, "10", "20", "25");
        Acao us = acao("IBM", "USD", Mercado.ESTADOS_UNIDOS, "2", "100", "110");
        AcaoRepository acoes = mock(AcaoRepository.class);
        CorretoraRepository corretoras = mock(CorretoraRepository.class);
        HistoricoCotacaoRepository historicos = mock(HistoricoCotacaoRepository.class);
        NegociacaoRepository negociacoes = mock(NegociacaoRepository.class);
        when(acoes.findAllByUsuarioIdOrderByTickerAsc(7L)).thenReturn(List.of(br, us));
        when(corretoras.countByUsuarioId(7L)).thenReturn(1L);
        when(historicos.findTop30ByAcaoIdOrderByDataHoraDesc(any())).thenReturn(List.of());
        when(negociacoes.findAllByAcaoUsuarioIdOrderByDataNegociacaoAscIdAsc(7L)).thenReturn(List.of());

        var dashboard = new DashboardService(acoes, corretoras, historicos, negociacoes, usuarioAtual()).dados();

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
        when(acoes.findAllByUsuarioIdOrderByTickerAsc(7L)).thenReturn(List.of(acao));
        when(historicos.findTop30ByAcaoIdOrderByDataHoraDesc(any())).thenReturn(List.of());
        when(negociacoes.findAllByAcaoUsuarioIdOrderByDataNegociacaoAscIdAsc(7L)).thenReturn(List.of(compra));

        var dashboard = new DashboardService(acoes, corretoras, historicos, negociacoes, usuarioAtual()).dados();

        assertThat(dashboard.precosMediosAcumulados()).singleElement().satisfies(serie -> {
            assertThat(serie.ticker()).isEqualTo("PETR4");
            assertThat(serie.labels()).containsExactly("Inicial", "01/09 10:00");
            assertThat(serie.valores()).containsExactly(new BigDecimal("100.0000"), new BigDecimal("110.0000"));
        });
    }

    @Test
    void deveManterPrecoMedioNoGraficoAposVendaComPrecoDiferente() {
        Acao acao = acao("PETR4", "BRL", Mercado.BRASIL, "6", "100", "120");
        Negociacao venda = new Negociacao();
        venda.setAcao(acao);
        venda.setTipo(TipoNegociacao.VENDA);
        venda.setQuantidade(new BigDecimal("4"));
        venda.setPrecoUnitario(new BigDecimal("130"));
        venda.setTaxas(BigDecimal.ZERO);
        venda.setValorBruto(new BigDecimal("520"));
        venda.setValorLiquido(new BigDecimal("520"));
        venda.setResultadoRealizado(new BigDecimal("120"));
        venda.setDataNegociacao(LocalDateTime.of(2026, 9, 2, 10, 0));

        AcaoRepository acoes = mock(AcaoRepository.class);
        CorretoraRepository corretoras = mock(CorretoraRepository.class);
        HistoricoCotacaoRepository historicos = mock(HistoricoCotacaoRepository.class);
        NegociacaoRepository negociacoes = mock(NegociacaoRepository.class);
        when(acoes.findAllByUsuarioIdOrderByTickerAsc(7L)).thenReturn(List.of(acao));
        when(historicos.findTop30ByAcaoIdOrderByDataHoraDesc(any())).thenReturn(List.of());
        when(negociacoes.findAllByAcaoUsuarioIdOrderByDataNegociacaoAscIdAsc(7L)).thenReturn(List.of(venda));

        var dashboard = new DashboardService(acoes, corretoras, historicos, negociacoes, usuarioAtual()).dados();

        assertThat(dashboard.precosMediosAcumulados()).singleElement().satisfies(serie -> {
            assertThat(serie.labels()).containsExactly("Inicial", "02/09 10:00");
            assertThat(serie.valores()).containsExactly(new BigDecimal("100.0000"), new BigDecimal("100.0000"));
        });
    }

    @Test
    void deveIncluirTodasAsAcoesBrasileirasEAmericanasNosGraficosDePrecoMedio() {
        List<Acao> carteira = new ArrayList<>();
        for (int indice = 1; indice <= 6; indice++) {
            carteira.add(acao("BR" + indice, "BRL", Mercado.BRASIL, "10", "20", "25"));
        }
        for (int indice = 1; indice <= 5; indice++) {
            carteira.add(acao("US" + indice, "USD", Mercado.ESTADOS_UNIDOS, "2", "100", "110"));
        }
        AcaoRepository acoes = mock(AcaoRepository.class);
        CorretoraRepository corretoras = mock(CorretoraRepository.class);
        HistoricoCotacaoRepository historicos = mock(HistoricoCotacaoRepository.class);
        NegociacaoRepository negociacoes = mock(NegociacaoRepository.class);
        when(acoes.findAllByUsuarioIdOrderByTickerAsc(7L)).thenReturn(carteira);
        when(historicos.findTop30ByAcaoIdOrderByDataHoraDesc(any())).thenReturn(List.of());
        when(negociacoes.findAllByAcaoUsuarioIdOrderByDataNegociacaoAscIdAsc(7L)).thenReturn(List.of());

        var dashboard = new DashboardService(acoes, corretoras, historicos, negociacoes, usuarioAtual()).dados();

        assertThat(dashboard.precosMediosAcumulados()).hasSize(11);
        assertThat(dashboard.precosMediosAcumulados()).filteredOn(s -> s.moeda().equals("BRL")).hasSize(6);
        assertThat(dashboard.precosMediosAcumulados()).filteredOn(s -> s.moeda().equals("USD")).hasSize(5);
    }

    @Test
    void deveMontarSeriesEmOrdemCronologicaComDesempenhoSobreOPrimeiroPonto() {
        Acao acao = acao("PETR4", "BRL", Mercado.BRASIL, "10", "20", "25");
        ReflectionTestUtils.setField(acao, "id", 1L);
        AcaoRepository acoes = mock(AcaoRepository.class);
        HistoricoCotacaoRepository historicos = mock(HistoricoCotacaoRepository.class);
        NegociacaoRepository negociacoes = mock(NegociacaoRepository.class);
        when(acoes.findAllByUsuarioIdOrderByTickerAsc(7L)).thenReturn(List.of(acao));
        when(historicos.findTop30ByAcaoIdOrderByDataHoraDesc(1L)).thenReturn(List.of(
                new HistoricoCotacao(acao, new BigDecimal("110"), LocalDateTime.of(2026, 9, 2, 10, 0)),
                new HistoricoCotacao(acao, new BigDecimal("100"), LocalDateTime.of(2026, 9, 1, 10, 0))));

        var dashboard = new DashboardService(acoes, mock(CorretoraRepository.class), historicos, negociacoes,
                usuarioAtual()).dados();

        assertThat(dashboard.series()).singleElement()
                .satisfies(serie -> assertThat(serie.valores()).containsExactly(new BigDecimal("100"), new BigDecimal("110")));
        assertThat(dashboard.desempenhos()).singleElement().satisfies(serie -> {
            assertThat(serie.labels()).hasSize(2);
            assertThat(serie.percentuais()).containsExactly(new BigDecimal("0.0000"), new BigDecimal("10.0000"));
        });
    }

    @Test
    void desempenhoNaoDeveDividirPorZeroQuandoPrimeiroPontoForZero() {
        Acao acao = acao("PETR4", "BRL", Mercado.BRASIL, "10", "20", "25");
        ReflectionTestUtils.setField(acao, "id", 1L);
        AcaoRepository acoes = mock(AcaoRepository.class);
        HistoricoCotacaoRepository historicos = mock(HistoricoCotacaoRepository.class);
        when(acoes.findAllByUsuarioIdOrderByTickerAsc(7L)).thenReturn(List.of(acao));
        when(historicos.findTop30ByAcaoIdOrderByDataHoraDesc(1L)).thenReturn(List.of(
                new HistoricoCotacao(acao, new BigDecimal("50"), LocalDateTime.of(2026, 9, 2, 10, 0)),
                new HistoricoCotacao(acao, BigDecimal.ZERO, LocalDateTime.of(2026, 9, 1, 10, 0))));

        var dashboard = new DashboardService(acoes, mock(CorretoraRepository.class), historicos,
                mock(NegociacaoRepository.class), usuarioAtual()).dados();

        assertThat(dashboard.desempenhos().get(0).percentuais()).containsExactly(BigDecimal.ZERO, BigDecimal.ZERO);
    }

    @Test
    void ativoSemSaldoESemOperacoesDeveMostrarPontoAtual() {
        Acao acao = acao("PETR4", "BRL", Mercado.BRASIL, "0", "0", "25");
        AcaoRepository acoes = mock(AcaoRepository.class);
        when(acoes.findAllByUsuarioIdOrderByTickerAsc(7L)).thenReturn(List.of(acao));

        var dashboard = new DashboardService(acoes, mock(CorretoraRepository.class),
                mock(HistoricoCotacaoRepository.class), mock(NegociacaoRepository.class), usuarioAtual()).dados();

        assertThat(dashboard.precosMediosAcumulados()).singleElement().satisfies(serie -> {
            assertThat(serie.labels()).containsExactly("Atual");
            assertThat(serie.valores()).singleElement()
                    .satisfies(valor -> assertThat(valor).isEqualByComparingTo("0"));
        });
    }

    @Test
    void operacoesDeOutroAtivoComMesmoTickerNaoEntramNaSerie() {
        Acao primeira = acao("PETR4", "BRL", Mercado.BRASIL, "10", "100", "120");
        ReflectionTestUtils.setField(primeira, "id", 1L);
        Acao outra = acao("PETR4", "BRL", Mercado.BRASIL, "10", "100", "120");
        ReflectionTestUtils.setField(outra, "id", 2L);
        Negociacao compraDaOutra = new Negociacao();
        compraDaOutra.setAcao(outra);
        compraDaOutra.setTipo(TipoNegociacao.COMPRA);
        compraDaOutra.setQuantidade(new BigDecimal("10"));
        compraDaOutra.setValorLiquido(new BigDecimal("1000"));
        compraDaOutra.setResultadoRealizado(BigDecimal.ZERO);
        compraDaOutra.setDataNegociacao(LocalDateTime.of(2026, 9, 1, 10, 0));
        AcaoRepository acoes = mock(AcaoRepository.class);
        NegociacaoRepository negociacoes = mock(NegociacaoRepository.class);
        when(acoes.findAllByUsuarioIdOrderByTickerAsc(7L)).thenReturn(List.of(primeira));
        when(negociacoes.findAllByAcaoUsuarioIdOrderByDataNegociacaoAscIdAsc(7L)).thenReturn(List.of(compraDaOutra));

        var dashboard = new DashboardService(acoes, mock(CorretoraRepository.class),
                mock(HistoricoCotacaoRepository.class), negociacoes, usuarioAtual()).dados();

        assertThat(dashboard.precosMediosAcumulados()).singleElement()
                .satisfies(serie -> assertThat(serie.labels()).containsExactly("Inicial"));
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

    private UsuarioAtualService usuarioAtual() {
        UsuarioAtualService usuarioAtual = mock(UsuarioAtualService.class);
        when(usuarioAtual.id()).thenReturn(7L);
        return usuarioAtual;
    }
}
