package br.com.carteira.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "historicos_cotacao", indexes = @Index(name = "idx_historico_acao_data", columnList = "acao_id,dataHora"))
public class HistoricoCotacao {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acao_id", nullable = false)
    private Acao acao;
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal valor;
    @Column(nullable = false)
    private LocalDateTime dataHora;

    public HistoricoCotacao() {}

    public HistoricoCotacao(Acao acao, BigDecimal valor, LocalDateTime dataHora) {
        this.acao = acao;
        this.valor = valor;
        this.dataHora = dataHora;
    }

    public Long getId() { return id; }
    public Acao getAcao() { return acao; }
    public BigDecimal getValor() { return valor; }
    public LocalDateTime getDataHora() { return dataHora; }
}
