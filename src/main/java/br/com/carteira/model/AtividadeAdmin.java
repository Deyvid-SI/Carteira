package br.com.carteira.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/** Guarda os e-mails como texto para o registro continuar legível mesmo se a conta mudar de e-mail. */
@Entity
@Table(name = "atividades_admin")
public class AtividadeAdmin {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 160)
    private String responsavel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AcaoAdmin acao;

    @Column(nullable = false, length = 160)
    private String conta;

    private String detalhe;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataHora;

    public AtividadeAdmin() {}

    public AtividadeAdmin(String responsavel, AcaoAdmin acao, String conta, String detalhe) {
        this.responsavel = responsavel;
        this.acao = acao;
        this.conta = conta;
        this.detalhe = detalhe;
        this.dataHora = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public String getResponsavel() { return responsavel; }
    public AcaoAdmin getAcao() { return acao; }
    public String getConta() { return conta; }
    public String getDetalhe() { return detalhe; }
    public LocalDateTime getDataHora() { return dataHora; }
}
