package br.com.carteira.dto;

import br.com.carteira.model.Papel;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Alteração cadastral sem senha: a troca de credencial tem endpoint próprio. */
public record UsuarioUpdateRequest(
        @NotBlank(message = "Informe o nome")
        @Size(min = 3, max = 100, message = "O nome deve ter entre 3 e 100 caracteres")
        String nome,

        @NotBlank(message = "Informe o e-mail")
        @Email(message = "Informe um e-mail válido")
        @Size(max = 160, message = "O e-mail deve ter no máximo 160 caracteres")
        String email,

        @NotNull(message = "Informe o papel")
        Papel papel,

        @NotNull(message = "Informe se a conta está ativa")
        Boolean ativo) {
}
