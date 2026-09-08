package br.com.carteira.integration;

import br.com.carteira.exception.ServicoExternoException;
import br.com.carteira.integration.gateway.CepGateway;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class ViaCepClient implements CepGateway {
    private final RestClient client;

    public ViaCepClient(RestClient.Builder builder) {
        this.client = builder.baseUrl("https://viacep.com.br").build();
    }

    @Override
    public EnderecoDados buscarPorCep(String cep) {
        try {
            ViaCepResponse response = client.get().uri("/ws/{cep}/json/", cep)
                    .retrieve().body(ViaCepResponse.class);
            if (response == null || Boolean.TRUE.equals(response.erro) || response.cep == null) {
                throw new ServicoExternoException("ViaCEP", "CEP não encontrado.");
            }
            return new EnderecoDados(response.cep.replaceAll("\\D", ""), response.logradouro,
                    response.complemento, response.bairro, response.localidade, response.uf);
        } catch (ServicoExternoException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ServicoExternoException("ViaCEP", "Não foi possível consultar o CEP agora.", ex);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record ViaCepResponse(String cep, String logradouro, String complemento, String bairro,
                                  String localidade, String uf, Boolean erro) {}
}
