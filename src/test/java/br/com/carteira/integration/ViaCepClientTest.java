package br.com.carteira.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.carteira.exception.ServicoExternoException;
import br.com.carteira.integration.gateway.CepGateway.EnderecoDados;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class ViaCepClientTest {
    private static final String URL = "https://viacep.com.br/ws/22440032/json/";

    private MockRestServiceServer server;
    private ViaCepClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new ViaCepClient(builder);
    }

    @Test
    void deveConverterEnderecoMantendoSomenteDigitosNoCep() {
        server.expect(requestTo(URL)).andRespond(withSuccess("""
                {"cep":"22440-032","logradouro":"Avenida Ataulfo de Paiva","complemento":"lado impar",
                 "bairro":"Leblon","localidade":"Rio de Janeiro","uf":"RJ","ibge":"3304557"}
                """, MediaType.APPLICATION_JSON));

        assertThat(client.buscarPorCep("22440032")).isEqualTo(new EnderecoDados("22440032",
                "Avenida Ataulfo de Paiva", "lado impar", "Leblon", "Rio de Janeiro", "RJ"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"erro\":true}", "{\"erro\":true,\"cep\":\"22440-032\"}", "{\"logradouro\":\"sem cep\"}"})
    void deveInformarCepInexistente(String corpo) {
        server.expect(requestTo(URL)).andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.buscarPorCep("22440032"))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessage("CEP não encontrado.");
    }

    @Test
    void deveInformarCepInexistenteQuandoCorpoVierVazio() {
        server.expect(requestTo(URL)).andRespond(withSuccess());

        assertThatThrownBy(() -> client.buscarPorCep("22440032"))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessage("CEP não encontrado.");
    }

    @Test
    void deveConverterFalhaDoServidor() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.buscarPorCep("22440032"))
                .isInstanceOfSatisfying(ServicoExternoException.class, ex -> {
                    assertThat(ex.getServico()).isEqualTo("ViaCEP");
                    assertThat(ex.getMessage()).isEqualTo("Não foi possível consultar o CEP agora.");
                });
    }
}
