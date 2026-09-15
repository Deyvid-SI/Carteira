package br.com.carteira.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "negociacoes", indexes = {
        @Index(name = "idx_negociacao_acao_data", columnList = "acao_id,data_negociacao"),
        @Index(name = "idx_negociacao_tipo", columnList = "tipo")
})
public class Negociacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private TipoNegociacao tipo;

    @Column(nullable = false, precision = 19, scale = 0)
    private BigDecimal quantidade;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal precoUnitario;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal taxas;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal valorBruto;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal valorLiquido;

    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal resultadoRealizado;

    @Column(nullable = false)
    private LocalDateTime dataNegociacao;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @PrePersist
    void prePersist() {
        dataCadastro = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public Acao getAcao() { return acao; }
    public void setAcao(Acao acao) { this.acao = acao; }
    public TipoNegociacao getTipo() { return tipo; }
    public void setTipo(TipoNegociacao tipo) { this.tipo = tipo; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPrecoUnitario() { return precoUnitario; }
    public void setPrecoUnitario(BigDecimal precoUnitario) { this.precoUnitario = precoUnitario; }
    public BigDecimal getTaxas() { return taxas; }
    public void setTaxas(BigDecimal taxas) { this.taxas = taxas; }
    public BigDecimal getValorBruto() { return valorBruto; }
    public void setValorBruto(BigDecimal valorBruto) { this.valorBruto = valorBruto; }
    public BigDecimal getValorLiquido() { return valorLiquido; }
    public void setValorLiquido(BigDecimal valorLiquido) { this.valorLiquido = valorLiquido; }
    public BigDecimal getResultadoRealizado() { return resultadoRealizado; }
    public void setResultadoRealizado(BigDecimal resultadoRealizado) { this.resultadoRealizado = resultadoRealizado; }
    public LocalDateTime getDataNegociacao() { return dataNegociacao; }
    public void setDataNegociacao(LocalDateTime dataNegociacao) { this.dataNegociacao = dataNegociacao; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
}
