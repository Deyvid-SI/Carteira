package br.com.carteira.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.carteira.exception.ServicoExternoException;
import br.com.carteira.integration.gateway.CotacaoGateway.CotacaoDados;
import br.com.carteira.model.Mercado;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

class AlphaVantageCotacaoClientTest {
    private static final String URL =
            "https://www.alphavantage.co/query?function=GLOBAL_QUOTE&symbol=IBM&apikey=chave-teste";

    private MockRestServiceServer server;
    private AlphaVantageCotacaoClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new AlphaVantageCotacaoClient(builder, "chave-teste");
    }

    @Test
    void deveAtenderSomenteOMercadoAmericano() {
        assertThat(client.suporta(Mercado.ESTADOS_UNIDOS)).isTrue();
        assertThat(client.suporta(Mercado.BRASIL)).isFalse();
    }

    @Test
    void deveConverterCotacaoEmDolar() {
        responder("""
                {"Global Quote":{"01. symbol":"IBM","05. price":"210.50",
                  "07. latest trading day":"2026-10-20","10. change percent":"1.2500%"}}
                """);

        CotacaoDados cotacao = client.buscar("IBM");

        assertThat(cotacao.ticker()).isEqualTo("IBM");
        assertThat(cotacao.moeda()).isEqualTo("USD");
        assertThat(cotacao.preco()).isEqualByComparingTo("210.50");
        assertThat(cotacao.variacaoPercentual()).isEqualByComparingTo("1.25");
        assertThat(cotacao.dataHora()).isEqualTo(LocalDateTime.of(2026, 10, 20, 18, 0));
        assertThat(cotacao.historico()).isEmpty();
        server.verify();
    }

    @Test
    void deveTolerarVariacaoEDataIlegiveis() {
        responder("""
                {"Global Quote":{"01. symbol":"IBM","05. price":"210.50",
                  "07. latest trading day":"ontem","10. change percent":"n/d"}}
                """);

        CotacaoDados cotacao = client.buscar("IBM");

        assertThat(cotacao.variacaoPercentual()).isEqualByComparingTo("0");
        assertThat(cotacao.dataHora()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"Note\":\"limite\"}", "{\"Information\":\"chave demo\"}"})
    void deveExplicarLimiteAtingido(String corpo) {
        responder(corpo);

        assertThatThrownBy(() -> client.buscar("IBM"))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("Defina ALPHA_VANTAGE_API_KEY");
    }

    @Test
    void deveExplicarLimiteQuandoCorpoVierVazio() {
        server.expect(requestTo(URL)).andRespond(withSuccess());

        assertThatThrownBy(() -> client.buscar("IBM"))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("Limite da Alpha Vantage");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{\"Global Quote\":{}}",
            "{\"Global Quote\":{\"01. symbol\":\"IBM\"}}",
            "{\"Global Quote\":{\"05. price\":\"10\"}}"})
    void deveInformarTickerInexistente(String corpo) {
        responder(corpo);

        assertThatThrownBy(() -> client.buscar("IBM"))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessage("Ticker americano não encontrado: IBM");
    }

    @Test
    void deveTratarPrecoIlegivelComoFalhaDoServico() {
        responder("{\"Global Quote\":{\"01. symbol\":\"IBM\",\"05. price\":\"abc\"}}");

        assertThatThrownBy(() -> client.buscar("IBM"))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessage("Não foi possível consultar a cotação americana agora.");
    }

    @Test
    void deveConverterFalhaHttpEmErroDeServicoExterno() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.buscar("IBM"))
                .isInstanceOfSatisfying(ServicoExternoException.class,
                        ex -> assertThat(ex.getServico()).isEqualTo("Alpha Vantage"));
    }

    private void responder(String corpo) {
        server.expect(requestTo(URL)).andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));
    }
}
