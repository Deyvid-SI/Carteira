package br.com.carteira.dto;

import br.com.carteira.model.Acao;
import br.com.carteira.model.Mercado;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AcaoResponse(Long id, String ticker, String nomeEmpresa, Mercado mercado, String moeda,
                           BigDecimal cotacaoAtual, BigDecimal variacaoPercentual,
                           BigDecimal quantidade, BigDecimal precoMedio, BigDecimal valorInvestido,
                           BigDecimal valorAtual, BigDecimal resultado, BigDecimal rentabilidade, BigDecimal resultadoDia,
                           LocalDateTime dataHoraCotacao, LocalDateTime dataCadastro) {
    public static AcaoResponse from(Acao a) {
        return new AcaoResponse(a.getId(), a.getTicker(), a.getNomeEmpresa(), a.getMercado(), a.getMoeda(),
                a.getCotacaoAtual(), a.getVariacaoPercentual(), a.getQuantidade(), a.getPrecoMedio(),
                a.getValorInvestido(), a.getValorAtual(), a.getResultado(), a.getRentabilidade(), a.getResultadoDia(),
                a.getDataHoraCotacao(), a.getDataCadastro());
    }
}
