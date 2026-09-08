package br.com.carteira.integration.gateway;

public interface EmpresaGateway {
    EmpresaDados buscarPorCnpj(String cnpj);

    record EmpresaDados(String cnpj, String razaoSocial, String nomeFantasia, String email,
                        String telefone, String cep, String situacaoCadastral) {}
}
