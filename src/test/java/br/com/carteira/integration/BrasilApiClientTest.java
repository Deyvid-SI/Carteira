package br.com.carteira.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.carteira.exception.ServicoExternoException;
import br.com.carteira.integration.gateway.CvmGateway.CvmDados;
import br.com.carteira.integration.gateway.EmpresaGateway.EmpresaDados;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class BrasilApiClientTest {
    private static final String CNPJ = "02332886000104";
    private static final String URL_CNPJ = "https://brasilapi.com.br/api/cnpj/v1/" + CNPJ;
    private static final String URL_CVM = "https://brasilapi.com.br/api/cvm/corretoras/v1/" + CNPJ;

    private MockRestServiceServer server;
    private BrasilApiClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BrasilApiClient(builder);
    }

    @Test
    void deveConverterDadosDaReceitaMantendoSomenteDigitosNoCep() {
        server.expect(requestTo(URL_CNPJ)).andRespond(withSuccess("""
                {"cnpj":"02332886000104","razao_social":"XP CORRETORA","nome_fantasia":"XP",
                 "email":"contato@xp.test","ddd_telefone_1":"2130000000","cep":"22440-032",
                 "descricao_situacao_cadastral":"ATIVA","campo_desconhecido":true}
                """, MediaType.APPLICATION_JSON));

        EmpresaDados empresa = client.buscarPorCnpj(CNPJ);

        assertThat(empresa).isEqualTo(new EmpresaDados(CNPJ, "XP CORRETORA", "XP", "contato@xp.test",
                "2130000000", "22440032", "ATIVA"));
    }

    @Test
    void deveAceitarEmpresaSemCep() {
        server.expect(requestTo(URL_CNPJ))
                .andRespond(withSuccess("{\"razao_social\":\"XP CORRETORA\"}", MediaType.APPLICATION_JSON));

        assertThat(client.buscarPorCnpj(CNPJ).cep()).isNull();
    }

    @Test
    void deveInformarCnpjInexistenteQuandoAReceitaResponder404() {
        server.expect(requestTo(URL_CNPJ)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.buscarPorCnpj(CNPJ))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessage("CNPJ não encontrado na Receita Federal.");
    }

    @Test
    void deveRecusarRespostaSemRazaoSocial() {
        server.expect(requestTo(URL_CNPJ)).andRespond(withSuccess("{\"cnpj\":\"1\"}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.buscarPorCnpj(CNPJ))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessage("A consulta de CNPJ retornou dados incompletos.");
    }

    @Test
    void deveRecusarCorpoVazioNaConsultaDeCnpj() {
        server.expect(requestTo(URL_CNPJ)).andRespond(withSuccess());

        assertThatThrownBy(() -> client.buscarPorCnpj(CNPJ))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessage("A consulta de CNPJ retornou dados incompletos.");
    }

    @Test
    void deveConverterFalhaDoServidorNaConsultaDeCnpj() {
        server.expect(requestTo(URL_CNPJ)).andRespond(withServerError());

        assertThatThrownBy(() -> client.buscarPorCnpj(CNPJ))
                .isInstanceOfSatisfying(ServicoExternoException.class, ex -> {
                    assertThat(ex.getServico()).isEqualTo("BrasilAPI/CNPJ");
                    assertThat(ex.getMessage()).isEqualTo("Não foi possível consultar o CNPJ agora.");
                });
    }

    @Test
    void deveConfirmarCorretoraRegistradaNaCvm() {
        server.expect(requestTo(URL_CVM)).andRespond(withSuccess(
                "{\"nome_social\":\"XP CORRETORA\",\"status\":\"EM FUNCIONAMENTO\"}", MediaType.APPLICATION_JSON));

        assertThat(client.validarCorretora(CNPJ)).isEqualTo(new CvmDados(true, "XP CORRETORA", "EM FUNCIONAMENTO"));
    }

    @Test
    void deveTratar404DaCvmComoCorretoraNaoRegistrada() {
        server.expect(requestTo(URL_CVM)).andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThat(client.validarCorretora(CNPJ)).isEqualTo(new CvmDados(false, null, "NÃO LOCALIZADA NA CVM"));
    }

    @Test
    void deveTratarCorpoVazioDaCvmComoCorretoraNaoRegistrada() {
        server.expect(requestTo(URL_CVM)).andRespond(withSuccess());

        assertThat(client.validarCorretora(CNPJ).registrada()).isFalse();
    }

    @Test
    void deveConverterFalhaDoServidorNaConsultaDaCvm() {
        server.expect(requestTo(URL_CVM)).andRespond(withServerError());

        assertThatThrownBy(() -> client.validarCorretora(CNPJ))
                .isInstanceOfSatisfying(ServicoExternoException.class, ex -> {
                    assertThat(ex.getServico()).isEqualTo("BrasilAPI/CVM");
                    assertThat(ex.getCause()).isNotNull();
                });
    }
}
