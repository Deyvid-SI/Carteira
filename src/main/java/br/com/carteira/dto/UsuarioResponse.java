package br.com.carteira.dto;

import br.com.carteira.model.Papel;
import br.com.carteira.model.Usuario;
import java.time.LocalDateTime;

/** Contrato público da conta. Não possui campo de senha para que o hash nunca seja serializado. */
public record UsuarioResponse(Long id, String nome, String email, Papel papel, boolean ativo,
                              LocalDateTime dataCadastro) {

    public static UsuarioResponse from(Usuario u) {
        return new UsuarioResponse(u.getId(), u.getNome(), u.getEmail(), u.getPapel(), u.isAtivo(),
                u.getDataCadastro());
    }
}
