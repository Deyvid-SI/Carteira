package br.com.carteira.integration;

import br.com.carteira.exception.ServicoExternoException;
import br.com.carteira.integration.gateway.CvmGateway;
import br.com.carteira.integration.gateway.EmpresaGateway;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BrasilApiClient implements EmpresaGateway, CvmGateway {
    private final RestClient client;

    public BrasilApiClient(RestClient.Builder builder) {
        this.client = builder.baseUrl("https://brasilapi.com.br").build();
    }

    @Override
    public EmpresaDados buscarPorCnpj(String cnpj) {
        try {
            EmpresaResponse response = client.get().uri("/api/cnpj/v1/{cnpj}", cnpj)
                    .retrieve()
                    .onStatus(HttpStatusCode::is4xxClientError, (req, res) -> {
                        throw new ServicoExternoException("BrasilAPI/CNPJ", "CNPJ não encontrado na Receita Federal.");
                    })
                    .body(EmpresaResponse.class);
            if (response == null || response.razaoSocial == null) {
                throw new ServicoExternoException("BrasilAPI/CNPJ", "A consulta de CNPJ retornou dados incompletos.");
            }
            return new EmpresaDados(response.cnpj, response.razaoSocial, response.nomeFantasia,
                    response.email, response.telefone, digits(response.cep), response.situacaoCadastral);
        } catch (ServicoExternoException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ServicoExternoException("BrasilAPI/CNPJ", "Não foi possível consultar o CNPJ agora.", ex);
        }
    }

    @Override
    public CvmDados validarCorretora(String cnpj) {
        try {
            CvmResponse response = client.get().uri("/api/cvm/corretoras/v1/{cnpj}", cnpj)
                    .retrieve().body(CvmResponse.class);
            return response == null
                    ? new CvmDados(false, null, "NÃO LOCALIZADA")
                    : new CvmDados(true, response.nomeSocial, response.status);
        } catch (HttpClientErrorException.NotFound ex) {
            return new CvmDados(false, null, "NÃO LOCALIZADA NA CVM");
        } catch (RestClientException ex) {
            throw new ServicoExternoException("BrasilAPI/CVM", "Não foi possível validar a corretora na CVM agora.", ex);
        }
    }

    private static String digits(String value) {
        return value == null ? null : value.replaceAll("\\D", "");
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record EmpresaResponse(
            String cnpj,
            @JsonProperty("razao_social") String razaoSocial,
            @JsonProperty("nome_fantasia") String nomeFantasia,
            String email,
            @JsonProperty("ddd_telefone_1") String telefone,
            String cep,
            @JsonProperty("descricao_situacao_cadastral") String situacaoCadastral) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    private record CvmResponse(@JsonProperty("nome_social") String nomeSocial, String status) {}
}
