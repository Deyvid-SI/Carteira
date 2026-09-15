package br.com.carteira.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "acoes", uniqueConstraints = @UniqueConstraint(
        name = "uk_acao_usuario_ticker", columnNames = {"usuario_id", "ticker"}))
public class Acao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;
    @Column(nullable = false, length = 20)
    private String ticker;
    @Column(nullable = false)
    private String nomeEmpresa;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Mercado mercado;
    @Column(nullable = false, length = 3)
    private String moeda;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal cotacaoAtual;
    @Column(precision = 10, scale = 4)
    private BigDecimal variacaoPercentual;
    @Column(precision = 19, scale = 0)
    private BigDecimal quantidade;
    @Column(precision = 19, scale = 4)
    private BigDecimal precoMedio;
    @Column(nullable = false)
    private LocalDateTime dataHoraCotacao;
    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @PrePersist
    void prePersist() { dataCadastro = LocalDateTime.now(); }

    public Long getId() { return id; }
    public Usuario getUsuario() { return usuario; }
    public void setUsuario(Usuario usuario) { this.usuario = usuario; }
    public String getTicker() { return ticker; }
    public void setTicker(String ticker) { this.ticker = ticker; }
    public String getNomeEmpresa() { return nomeEmpresa; }
    public void setNomeEmpresa(String nomeEmpresa) { this.nomeEmpresa = nomeEmpresa; }
    public Mercado getMercado() { return mercado; }
    public void setMercado(Mercado mercado) { this.mercado = mercado; }
    public String getMoeda() { return moeda; }
    public void setMoeda(String moeda) { this.moeda = moeda; }
    public BigDecimal getCotacaoAtual() { return cotacaoAtual; }
    public void setCotacaoAtual(BigDecimal cotacaoAtual) { this.cotacaoAtual = cotacaoAtual; }
    public BigDecimal getVariacaoPercentual() { return variacaoPercentual; }
    public void setVariacaoPercentual(BigDecimal variacaoPercentual) { this.variacaoPercentual = variacaoPercentual; }
    public BigDecimal getQuantidade() { return quantidade == null ? BigDecimal.ZERO : quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getPrecoMedio() { return precoMedio == null ? cotacaoAtual : precoMedio; }
    public void setPrecoMedio(BigDecimal precoMedio) { this.precoMedio = precoMedio; }
    public BigDecimal getValorInvestido() { return money(getPrecoMedio().multiply(getQuantidade())); }
    public BigDecimal getValorAtual() { return money(cotacaoAtual.multiply(getQuantidade())); }
    public BigDecimal getResultado() { return money(getValorAtual().subtract(getValorInvestido())); }
    public BigDecimal getRentabilidade() {
        BigDecimal investido = getValorInvestido();
        if (investido.signum() == 0) return BigDecimal.ZERO;
        return getResultado().multiply(BigDecimal.valueOf(100)).divide(investido, 4, RoundingMode.HALF_UP);
    }
    public BigDecimal getResultadoDia() {
        if (variacaoPercentual == null) return BigDecimal.ZERO.setScale(4);
        BigDecimal fator = BigDecimal.ONE.add(variacaoPercentual.divide(BigDecimal.valueOf(100), 10, RoundingMode.HALF_UP));
        if (fator.signum() == 0) return BigDecimal.ZERO.setScale(4);
        BigDecimal fechamentoAnterior = cotacaoAtual.divide(fator, 10, RoundingMode.HALF_UP);
        return money(cotacaoAtual.subtract(fechamentoAnterior).multiply(getQuantidade()));
    }
    public LocalDateTime getDataHoraCotacao() { return dataHoraCotacao; }
    public void setDataHoraCotacao(LocalDateTime dataHoraCotacao) { this.dataHoraCotacao = dataHoraCotacao; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }

    @Transient
    public String getLogoUrl() {
        if (ticker == null || ticker.isBlank()) return null;
        String simbolo = ticker.toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9.-]", "");
        if (simbolo.isBlank()) return null;
        if (mercado == Mercado.BRASIL && !simbolo.endsWith(".SA")) simbolo += ".SA";
        return "https://images.financialmodelingprep.com/symbol/" + simbolo + ".png";
    }

    private static BigDecimal money(BigDecimal value) { return value.setScale(4, RoundingMode.HALF_UP); }
}
