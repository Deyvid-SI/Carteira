package br.com.carteira.dto;

import br.com.carteira.model.Mercado;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import java.math.BigDecimal;

public class CadastrarAcaoRequest {
    @NotBlank(message = "Informe o ticker")
    @Pattern(regexp = "^[A-Za-z0-9.-]{1,20}$", message = "Ticker inválido")
    private String ticker;

    @NotNull(message = "Selecione o mercado")
    private Mercado mercado;

    @DecimalMin(value = "0.0000", message = "A quantidade não pode ser negativa")
    @Digits(integer = 15, fraction = 4, message = "Quantidade inválida")
    private BigDecimal quantidade = BigDecimal.ZERO;

    @DecimalMin(value = "0.0001", message = "O preço médio deve ser maior que zero")
    @Digits(integer = 15, fraction = 4, message = "Preço médio inválido")
    private BigDecimal precoMedio;

    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public Mercado getMercado() { return mercado; }
    public void setMercado(Mercado mercado) { this.mercado = mercado; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPrecoMedio() { return precoMedio; }
    public void setPrecoMedio(BigDecimal precoMedio) { this.precoMedio = precoMedio; }
}
