package br.com.carteira.model;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class AcaoCalculosTest {

    @Test
    void deveCalcularTodosOsValoresDaPosicao() {
        Acao acao = new Acao();
        acao.setQuantidade(new BigDecimal("10"));
        acao.setPrecoMedio(new BigDecimal("20"));
        acao.setCotacaoAtual(new BigDecimal("25"));
        acao.setVariacaoPercentual(new BigDecimal("5"));

        assertThat(acao.getValorInvestido()).isEqualByComparingTo("200.0000");
        assertThat(acao.getValorAtual()).isEqualByComparingTo("250.0000");
        assertThat(acao.getResultado()).isEqualByComparingTo("50.0000");
        assertThat(acao.getRentabilidade()).isEqualByComparingTo("25.0000");
        assertThat(acao.getResultadoDia()).isEqualByComparingTo("11.9048");
    }

    @Test
    void deveConsiderarSaldoZeroQuandoAtivoNaoPossuiPosicao() {
        Acao acao = new Acao();
        acao.setCotacaoAtual(new BigDecimal("42.50"));

        assertThat(acao.getQuantidade()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("42.50");
        assertThat(acao.getResultado()).isZero();
    }

    @Test
    void deveGerarLogoPeloTickerParaOsDoisMercados() {
        Acao brasileira = new Acao();
        brasileira.setTicker("PETR4");
        brasileira.setMercado(Mercado.BRASIL);
        Acao americana = new Acao();
        americana.setTicker("AAPL");
        americana.setMercado(Mercado.ESTADOS_UNIDOS);

        assertThat(brasileira.getLogoUrl()).endsWith("/PETR4.SA.png");
        assertThat(americana.getLogoUrl()).endsWith("/AAPL.png");
    }

    @Test
    void naoDeveDuplicarSufixoNemManterCaracteresInvalidosNoLogo() {
        Acao comSufixo = new Acao();
        comSufixo.setTicker("petr4.sa");
        comSufixo.setMercado(Mercado.BRASIL);
        Acao comSimbolos = new Acao();
        comSimbolos.setTicker("A/B C");
        comSimbolos.setMercado(Mercado.ESTADOS_UNIDOS);

        assertThat(comSufixo.getLogoUrl()).endsWith("/PETR4.SA.png");
        assertThat(comSimbolos.getLogoUrl()).endsWith("/ABC.png");
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "   ", "///"})
    void naoDeveGerarLogoSemSimboloValido(String ticker) {
        Acao acao = new Acao();
        acao.setTicker(ticker);

        assertThat(acao.getLogoUrl()).isNull();
    }

    @Test
    void rentabilidadeDeveSerZeroQuandoNadaFoiInvestido() {
        Acao acao = new Acao();
        acao.setQuantidade(BigDecimal.ZERO);
        acao.setPrecoMedio(BigDecimal.ZERO);
        acao.setCotacaoAtual(new BigDecimal("25"));

        assertThat(acao.getRentabilidade()).isZero();
    }

    @Test
    void resultadoDoDiaDeveSerZeroSemVariacaoInformada() {
        Acao acao = new Acao();
        acao.setQuantidade(new BigDecimal("10"));
        acao.setCotacaoAtual(new BigDecimal("25"));

        assertThat(acao.getResultadoDia()).isZero();
    }

    @Test
    void resultadoDoDiaNaoDeveDividirPorZeroComVariacaoDeMenosCemPorCento() {
        Acao acao = new Acao();
        acao.setQuantidade(new BigDecimal("10"));
        acao.setCotacaoAtual(new BigDecimal("25"));
        acao.setVariacaoPercentual(new BigDecimal("-100"));

        assertThat(acao.getResultadoDia()).isZero();
    }
}
