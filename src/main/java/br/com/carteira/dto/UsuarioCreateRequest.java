package br.com.carteira.dto;

import br.com.carteira.model.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UsuarioCreateRequest(
        @NotBlank(message = "Informe o nome")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        String nome,

        @NotBlank(message = "Informe o e-mail")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
        String email,

        @NotBlank(message = "Informe uma senha")
        @Pattern(regexp = SenhaPolitica.REGEX, message = SenhaPolitica.MENSAGEM)
        String senha,

        @NotNull(message = "Informe o papel")
        Papel papel) {
}
