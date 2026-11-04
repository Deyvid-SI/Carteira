package br.com.carteira.dto;

import java.time.LocalDateTime;

/** Somente totais do sistema: nenhum valor, ativo ou operação de uma conta específica. */
public record PainelAdmin(
        long totalContas,
        long contasAtivas,
        long contasDesativadas,
        long administradores,
        long contasNovas30Dias,
        long totalAtivos,
        long totalCorretoras,
        long totalNegociacoes,
        LocalDateTime ultimaCotacao,
        boolean brapiComToken,
        boolean alphaVantageComChavePropria,
        boolean consoleH2Ativo) {
}
