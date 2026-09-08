package br.com.carteira.dto;

import br.com.carteira.model.Mercado;
import br.com.carteira.model.Negociacao;
import br.com.carteira.model.TipoNegociacao;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record NegociacaoResponse(Long id, Long acaoId, String ticker, Mercado mercado, String moeda,
                                 TipoNegociacao tipo, BigDecimal quantidade, BigDecimal precoUnitario,
                                 BigDecimal taxas, BigDecimal valorBruto, BigDecimal valorLiquido,
                                 BigDecimal resultadoRealizado, LocalDateTime dataNegociacao,
                                 LocalDateTime dataCadastro) {
    public static NegociacaoResponse from(Negociacao negociacao) {
        return new NegociacaoResponse(
                negociacao.getId(), negociacao.getAcao().getId(), negociacao.getAcao().getTicker(),
                negociacao.getAcao().getMercado(), negociacao.getAcao().getMoeda(), negociacao.getTipo(),
                negociacao.getQuantidade(), negociacao.getPrecoUnitario(), negociacao.getTaxas(),
                negociacao.getValorBruto(), negociacao.getValorLiquido(), negociacao.getResultadoRealizado(),
                negociacao.getDataNegociacao(), negociacao.getDataCadastro());
    }
}
