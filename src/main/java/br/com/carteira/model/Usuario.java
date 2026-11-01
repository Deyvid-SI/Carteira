package br.com.carteira.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios", uniqueConstraints = @UniqueConstraint(name = "uk_usuario_email", columnNames = "email"))
public class Usuario {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(nullable = false, length = 160)
    private String email;

    @Column(nullable = false)
    private String senha;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Papel papel = Papel.USER;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @PrePersist
    void prePersist() { dataCadastro = LocalDateTime.now(); }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public Papel getPapel() { return papel; }
    public void setPapel(Papel papel) { this.papel = papel; }
    public boolean isAtivo() { return ativo; }
    public void setAtivo(boolean ativo) { this.ativo = ativo; }
    public LocalDateTime getDataCadastro() { return dataCadastro; }
}
