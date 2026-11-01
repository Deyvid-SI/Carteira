package br.com.carteira.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class CadastroUsuarioRequest {
    @NotBlank(message = "Informe seu nome")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    private String nome;

    @NotBlank(message = "Informe seu e-mail")
    @Email(message = "Informe um e-mail válido")
    private String email;

    @NotBlank(message = "Informe uma senha")
    @Pattern(regexp = SenhaPolitica.REGEX, message = SenhaPolitica.MENSAGEM)
    private String senha;

    @NotBlank(message = "Confirme sua senha")
    private String confirmarSenha;

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public String getConfirmarSenha() { return confirmarSenha; }
    public void setConfirmarSenha(String confirmarSenha) { this.confirmarSenha = confirmarSenha; }
}
