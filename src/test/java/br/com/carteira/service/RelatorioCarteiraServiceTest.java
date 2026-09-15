package br.com.carteira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import br.com.carteira.model.Acao;
import br.com.carteira.model.Mercado;
import br.com.carteira.model.Negociacao;
import br.com.carteira.model.TipoNegociacao;
import br.com.carteira.repository.NegociacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class RelatorioCarteiraServiceTest {
    private final RelatorioCarteiraService service = new RelatorioCarteiraService(
            mock(NegociacaoRepository.class), mock(UsuarioAtualService.class));

    @Test
    void deveConsolidarHistoricoSemMisturarMoedas() {
        Acao petr4 = acao("PETR4", Mercado.BRASIL);
        Acao aapl = acao("AAPL", Mercado.ESTADOS_UNIDOS);
        var compra = negociacao(petr4, TipoNegociacao.COMPRA, "1000.00", "0.00", 2025, 2);
        var venda = negociacao(petr4, TipoNegociacao.VENDA, "650.00", "150.00", 2025, 7);
        var vendaUsd = negociacao(aapl, TipoNegociacao.VENDA, "400.00", "-25.00", 2026, 1);

        var resumo = service.resumirHistorico(List.of(vendaUsd, venda, compra));

        assertThat(resumo.totalOperacoes()).isEqualTo(3);
        assertThat(resumo.compras()).isEqualTo(1);
        assertThat(resumo.vendas()).isEqualTo(2);
        assertThat(resumo.ativosMovimentados()).isEqualTo(2);
        assertThat(resumo.volumeBrl()).isEqualByComparingTo("1650.00");
        assertThat(resumo.volumeUsd()).isEqualByComparingTo("400.00");
        assertThat(resumo.resultadoRealizadoBrl()).isEqualByComparingTo("150.00");
        assertThat(resumo.resultadoRealizadoUsd()).isEqualByComparingTo("-25.00");
    }

    @Test
    void deveResumirImpostoApenasComValoresDasVendas() {
        Acao petr4 = acao("PETR4", Mercado.BRASIL);
        var compra = negociacao(petr4, TipoNegociacao.COMPRA, "1000.00", "0.00", 2026, 1);
        var venda = negociacao(petr4, TipoNegociacao.VENDA, "1400.00", "400.00", 2026, 8);

        var resumo = service.resumirAno(2026, List.of(compra, venda));

        assertThat(resumo.operacoes()).isEqualTo(2);
        assertThat(resumo.compras()).isEqualTo(1);
        assertThat(resumo.vendas()).isEqualTo(1);
        assertThat(resumo.vendasBrl()).isEqualByComparingTo("1400.00");
        assertThat(resumo.resultadoRealizadoBrl()).isEqualByComparingTo("400.00");
    }

    @Test
    void deveOrdenarResumosDoAnoMaisRecenteParaOMaisAntigo() {
        Acao petr4 = acao("PETR4", Mercado.BRASIL);
        var antiga = negociacao(petr4, TipoNegociacao.COMPRA, "10", "0", 2024, 1);
        var recente = negociacao(petr4, TipoNegociacao.VENDA, "12", "2", 2026, 1);

        assertThat(service.resumirPorAno(List.of(antiga, recente)))
                .extracting(resumo -> resumo.ano())
                .containsExactly(2026, 2024);
        assertThat(service.listarAnos(List.of(antiga, recente))).containsExactly(2026, 2024);
    }

    private Acao acao(String ticker, Mercado mercado) {
        Acao acao = new Acao();
        acao.setTicker(ticker);
        acao.setNomeEmpresa(ticker + " Empresa");
        acao.setMercado(mercado);
        acao.setMoeda(mercado.getMoedaPadrao());
        acao.setCotacaoAtual(BigDecimal.TEN);
        acao.setQuantidade(BigDecimal.TEN);
        acao.setPrecoMedio(BigDecimal.TEN);
        acao.setDataHoraCotacao(LocalDateTime.now());
        return acao;
    }

    private Negociacao negociacao(Acao acao, TipoNegociacao tipo, String bruto, String resultado,
                                   int ano, int mes) {
        Negociacao negociacao = new Negociacao();
        negociacao.setAcao(acao);
        negociacao.setTipo(tipo);
        negociacao.setQuantidade(BigDecimal.ONE);
        negociacao.setPrecoUnitario(new BigDecimal(bruto));
        negociacao.setTaxas(BigDecimal.ZERO);
        negociacao.setValorBruto(new BigDecimal(bruto));
        negociacao.setValorLiquido(new BigDecimal(bruto));
        negociacao.setResultadoRealizado(new BigDecimal(resultado));
        negociacao.setDataNegociacao(LocalDateTime.of(ano, mes, 10, 12, 0));
        return negociacao;
    }
}
