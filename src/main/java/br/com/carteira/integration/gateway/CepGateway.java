package br.com.carteira.integration.gateway;

public interface CepGateway {
    EnderecoDados buscarPorCep(String cep);

    record EnderecoDados(String cep, String logradouro, String complemento, String bairro,
                         String cidade, String uf) {}
}
