package br.com.carteira.dto;

import br.com.carteira.model.TipoNegociacao;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;

public class RegistrarNegociacaoRequest {
    @NotNull(message = "Selecione um ativo.")
    private Long acaoId;

    @NotNull(message = "Selecione compra ou venda.")
    private TipoNegociacao tipo;

    @NotNull(message = "Informe a quantidade.")
    @DecimalMin(value = "1", message = "A quantidade deve ser maior que zero.")
    @Digits(integer = 15, fraction = 0, message = "A quantidade deve ser um número inteiro.")
    private BigDecimal quantidade;

    @NotNull(message = "Informe o preço da compra ou venda.")
    @DecimalMin(value = "0.0001", message = "O preço deve ser maior que zero.")
    @Digits(integer = 15, fraction = 4, message = "Preço inválido.")
    private BigDecimal precoUnitario;

    @NotNull(message = "Informe a data da negociação.")
    @PastOrPresent(message = "A data da negociação não pode estar no futuro.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime dataNegociacao = LocalDateTime.now().withSecond(0).withNano(0);

    public Long getAcaoId() { return acaoId; }
    public void setAcaoId(Long acaoId) { this.acaoId = acaoId; }
    public TipoNegociacao getTipo() { return tipo; }
    public void setTipo(TipoNegociacao tipo) { this.tipo = tipo; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
    public LocalDateTime getDataNegociacao() { return dataNegociacao; }
    public void setDataNegociacao(LocalDateTime dataNegociacao) { this.dataNegociacao = dataNegociacao; }
}
