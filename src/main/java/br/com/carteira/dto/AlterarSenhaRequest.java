package br.com.carteira.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record AlterarSenhaRequest(
        @NotBlank(message = "Informe a nova senha")
        @Pattern(regexp = SenhaPolitica.REGEX, message = SenhaPolitica.MENSAGEM)
        String senha) {
}
