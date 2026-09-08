package br.com.carteira.model;

import static org.assertj.core.api.Assertions.assertThat;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

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
    void deveManterCompatibilidadeComAtivoSemPosicaoAnterior() {
        Acao acao = new Acao();
        acao.setCotacaoAtual(new BigDecimal("42.50"));

        assertThat(acao.getQuantidade()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("42.50");
        assertThat(acao.getResultado()).isZero();
    }
}
