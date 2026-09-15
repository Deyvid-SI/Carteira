package br.com.carteira.dto;

import java.math.BigDecimal;

public record ResumoFiscalAno(
        int ano,
        long operacoes,
        long compras,
        long vendas,
        BigDecimal vendasBrl,
        BigDecimal vendasUsd,
        BigDecimal resultadoRealizadoBrl,
        BigDecimal resultadoRealizadoUsd) {

    public static ResumoFiscalAno vazio(int ano) {
        return new ResumoFiscalAno(ano, 0, 0, 0, BigDecimal.ZERO, BigDecimal.ZERO,
                BigDecimal.ZERO, BigDecimal.ZERO);
    }
}
