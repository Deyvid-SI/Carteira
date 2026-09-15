package br.com.carteira.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record HistoricoResumo(
        int totalOperacoes,
        long compras,
        long vendas,
        long ativosMovimentados,
        BigDecimal volumeBrl,
        BigDecimal volumeUsd,
        BigDecimal resultadoRealizadoBrl,
        BigDecimal resultadoRealizadoUsd,
        LocalDateTime primeiraOperacao,
        LocalDateTime ultimaOperacao) {
}
