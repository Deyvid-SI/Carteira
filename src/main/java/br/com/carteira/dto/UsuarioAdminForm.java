package br.com.carteira.dto;

import br.com.carteira.model.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class UsuarioAdminForm {
    @NotBlank(message = "Informe o nome")
    @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
    private String nome;

    @NotBlank(message = "Informe o e-mail")
    @Email(message = "Informe um e-mail válido")
    @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
    private String email;

    @NotBlank(message = "Informe uma senha")
    @Pattern(regexp = SenhaPolitica.REGEX, message = SenhaPolitica.MENSAGEM)
    private String senha;

    @NotNull(message = "Selecione o papel")
    private Papel papel = Papel.USER;

    public UsuarioCreateRequest paraRequest() { return new UsuarioCreateRequest(nome, email, senha, papel); }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public Papel getPapel() { return papel; }
    public void setPapel(Papel papel) { this.papel = papel; }
}
