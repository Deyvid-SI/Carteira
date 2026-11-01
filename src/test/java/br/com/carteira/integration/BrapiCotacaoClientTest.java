package br.com.carteira.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import br.com.carteira.exception.ServicoExternoException;
import br.com.carteira.integration.gateway.CotacaoGateway.CotacaoDados;
import br.com.carteira.integration.gateway.CotacaoGateway.PontoHistorico;
import br.com.carteira.model.Mercado;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

/** A brapi é uma fronteira externa: o servidor simulado evita rede real e mantém o teste repetível. */
class BrapiCotacaoClientTest {
    private static final String URL = "https://brapi.dev/api/quote/PETR4?range=1mo&interval=1d";

    private MockRestServiceServer server;
    private BrapiCotacaoClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        client = new BrapiCotacaoClient(builder, "");
    }

    @Test
    void deveAtenderSomenteOMercadoBrasileiro() {
        assertThat(client.suporta(Mercado.BRASIL)).isTrue();
        assertThat(client.suporta(Mercado.ESTADOS_UNIDOS)).isFalse();
    }

    @Test
    void deveConverterRespostaCompleta() {
        responder("""
                {"results":[{"symbol":"PETR4","longName":"Petrobras S.A.","shortName":"PETROBRAS","currency":"BRL",
                  "regularMarketPrice":38.5,"regularMarketChangePercent":-1.25,
                  "regularMarketTime":"2026-10-20T17:00:00-03:00",
                  "historicalDataPrice":[{"date":1760000000,"close":37.9},{"date":1760086400,"close":null}]}]}
                """);

        CotacaoDados cotacao = client.buscar("PETR4");

        assertThat(cotacao.ticker()).isEqualTo("PETR4");
        assertThat(cotacao.nomeEmpresa()).isEqualTo("Petrobras S.A.");
        assertThat(cotacao.moeda()).isEqualTo("BRL");
        assertThat(cotacao.preco()).isEqualByComparingTo("38.5");
        assertThat(cotacao.variacaoPercentual()).isEqualByComparingTo("-1.25");
        assertThat(cotacao.dataHora()).isEqualTo(LocalDateTime.of(2026, 10, 20, 17, 0));
        assertThat(cotacao.historico()).as("ponto sem fechamento é descartado").hasSize(1);
        assertThat(cotacao.historico().get(0).valor()).isEqualByComparingTo("37.9");
        server.verify();
    }

    @Test
    void deveUsarValoresPadraoQuandoCamposOpcionaisFaltam() {
        responder("{\"results\":[{\"regularMarketPrice\":10}]}");

        CotacaoDados cotacao = client.buscar("PETR4");

        assertThat(cotacao.ticker()).isEqualTo("PETR4");
        assertThat(cotacao.nomeEmpresa()).isEqualTo("PETR4");
        assertThat(cotacao.moeda()).isEqualTo("BRL");
        assertThat(cotacao.variacaoPercentual()).isNull();
        assertThat(cotacao.historico()).isEmpty();
        assertThat(cotacao.dataHora()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void deveUsarNomeCurtoQuandoNomeLongoFalta() {
        responder("{\"results\":[{\"symbol\":\"PETR4\",\"shortName\":\"PETROBRAS\",\"regularMarketPrice\":10}]}");

        assertThat(client.buscar("PETR4").nomeEmpresa()).isEqualTo("PETROBRAS");
    }

    @Test
    void deveLerFormatoAninhadoComHistoricoForaDoBlocoData() {
        responder("""
                {"results":[{"symbol":"PETR4",
                  "data":{"regularMarketPrice":"38.50%","longName":"Petrobras S.A."},
                  "historicalDataPrice":[{"date":"2026-10-01","close":"37.5"}]}]}
                """);

        CotacaoDados cotacao = client.buscar("PETR4");

        assertThat(cotacao.ticker()).as("símbolo vem do resultado externo").isEqualTo("PETR4");
        assertThat(cotacao.preco()).isEqualByComparingTo("38.50");
        assertThat(cotacao.historico()).extracting(PontoHistorico::dataHora)
                .containsExactly(LocalDateTime.of(2026, 10, 1, 0, 0));
    }

    @Test
    void devePreferirHistoricoDoBlocoDataQuandoExiste() {
        responder("""
                {"results":[{"data":{"regularMarketPrice":10,"historicalDataPrice":[{"date":"2026-10-02","close":11}]},
                  "historicalDataPrice":[{"date":"2026-10-01","close":99}]}]}
                """);

        assertThat(client.buscar("PETR4").historico()).singleElement()
                .satisfies(ponto -> assertThat(ponto.valor()).isEqualByComparingTo("11"));
    }

    @Test
    void deveInterpretarDataEmSegundosEMilissegundosNoLimiteDeDezDigitos() {
        responder("""
                {"results":[{"regularMarketPrice":10,"historicalDataPrice":[
                  {"date":9999999999,"close":1},{"date":10000000000,"close":2}]}]}
                """);

        var historico = client.buscar("PETR4").historico();

        assertThat(historico.get(0).dataHora()).isEqualTo(deEpoch(9_999_999_999L));
        assertThat(historico.get(1).dataHora()).as("11 dígitos são milissegundos").isEqualTo(deEpoch(10_000_000L));
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"ontem\"", "null"})
    void deveAssumirAgoraQuandoDataForInvalidaOuNula(String data) {
        responder("{\"results\":[{\"regularMarketPrice\":10,\"regularMarketTime\":" + data + "}]}");

        assertThat(client.buscar("PETR4").dataHora()).isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void deveIgnorarHistoricoQueNaoSejaLista() {
        responder("{\"results\":[{\"regularMarketPrice\":10,\"historicalDataPrice\":\"indisponivel\"}]}");

        assertThat(client.buscar("PETR4").historico()).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"{\"results\":[]}", "{\"results\":[{}]}", "{}"})
    void deveInformarTickerInexistente(String corpo) {
        responder(corpo);

        assertThatThrownBy(() -> client.buscar("PETR4"))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("Ticker brasileiro não encontrado: PETR4");
    }

    @Test
    void deveInformarTickerInexistenteQuandoCorpoVierVazio() {
        server.expect(requestTo(URL)).andRespond(withSuccess());

        assertThatThrownBy(() -> client.buscar("PETR4"))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessageContaining("não encontrado");
    }

    @ParameterizedTest
    @ValueSource(strings = {"\"\"", "\"38,50\"", "null"})
    void deveRecusarCotacaoAusenteOuIlegivel(String preco) {
        responder("{\"results\":[{\"symbol\":\"PETR4\",\"regularMarketPrice\":" + preco + "}]}");

        assertThatThrownBy(() -> client.buscar("PETR4"))
                .isInstanceOf(ServicoExternoException.class)
                .hasMessage("A brapi não retornou uma cotação para PETR4.");
    }

    @Test
    void deveConverterFalhaHttpEmErroDeServicoExterno() {
        server.expect(requestTo(URL)).andRespond(withServerError());

        assertThatThrownBy(() -> client.buscar("PETR4"))
                .isInstanceOfSatisfying(ServicoExternoException.class, ex -> {
                    assertThat(ex.getServico()).isEqualTo("brapi");
                    assertThat(ex.getMessage()).isEqualTo("Não foi possível consultar a cotação brasileira agora.");
                    assertThat(ex.getCause()).isNotNull();
                });
    }

    @Test
    void deveEnviarTokenSomenteQuandoConfigurado() {
        server.expect(requestTo(URL)).andExpect(headerDoesNotExist("Authorization"))
                .andRespond(withSuccess("{\"results\":[{\"regularMarketPrice\":1}]}", MediaType.APPLICATION_JSON));
        client.buscar("PETR4");
        server.verify();

        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer comToken = MockRestServiceServer.bindTo(builder).build();
        comToken.expect(requestTo(URL)).andExpect(header("Authorization", "Bearer token-brapi"))
                .andRespond(withSuccess("{\"results\":[{\"regularMarketPrice\":1}]}", MediaType.APPLICATION_JSON));
        new BrapiCotacaoClient(builder, "token-brapi").buscar("PETR4");
        comToken.verify();
    }

    private void responder(String corpo) {
        server.expect(requestTo(URL)).andRespond(withSuccess(corpo, MediaType.APPLICATION_JSON));
    }

    private static LocalDateTime deEpoch(long segundos) {
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(segundos), ZoneId.systemDefault());
    }
}
