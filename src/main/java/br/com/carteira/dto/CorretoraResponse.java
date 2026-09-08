package br.com.carteira.dto;

import br.com.carteira.model.Corretora;
import java.time.LocalDateTime;

public record CorretoraResponse(
        Long id, String cnpj, String razaoSocial, String nomeFantasia, String email, String telefone,
        String cep, String logradouro, String numero, String complemento, String bairro, String cidade,
        String uf, String situacaoCadastral, boolean validadaNaCvm, String statusCvm, LocalDateTime dataCadastro) {

    public static CorretoraResponse from(Corretora c) {
        return new CorretoraResponse(c.getId(), c.getCnpj(), c.getRazaoSocial(), c.getNomeFantasia(),
                c.getEmail(), c.getTelefone(), c.getCep(), c.getLogradouro(), c.getNumero(),
                c.getComplemento(), c.getBairro(), c.getCidade(), c.getUf(), c.getSituacaoCadastral(),
                c.isValidadaNaCvm(), c.getStatusCvm(), c.getDataCadastro());
    }
}
