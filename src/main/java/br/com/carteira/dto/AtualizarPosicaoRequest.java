package br.com.carteira.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class AtualizarPosicaoRequest {
    @NotNull(message = "Informe a quantidade")
    @DecimalMin(value = "0.0001", message = "A quantidade deve ser maior que zero")
    @Digits(integer = 15, fraction = 4, message = "Quantidade inválida")
    private BigDecimal quantidade;

    @NotNull(message = "Informe o preço médio")
    @DecimalMin(value = "0.0001", message = "O preço médio deve ser maior que zero")
    @Digits(integer = 15, fraction = 4, message = "Preço médio inválido")
    private BigDecimal precoMedio;

    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPrecoMedio() { return precoMedio; }
    public void setPrecoMedio(BigDecimal precoMedio) { this.precoMedio = precoMedio; }
}
