package br.com.carteira.integration;

import br.com.carteira.exception.ServicoExternoException;
import br.com.carteira.integration.gateway.CotacaoGateway;
import br.com.carteira.model.Mercado;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AlphaVantageCotacaoClient implements CotacaoGateway {
    private final RestClient client;
    private final String apiKey;

    public AlphaVantageCotacaoClient(RestClient.Builder builder,
                                     @Value("${integracoes.alpha-vantage.api-key:demo}") String apiKey) {
        this.client = builder.baseUrl("https://www.alphavantage.co").build();
        this.apiKey = apiKey;
    }

    @Override
    public boolean suporta(Mercado mercado) { return mercado == Mercado.ESTADOS_UNIDOS; }

    @Override
    public CotacaoDados buscar(String ticker) {
        try {
            JsonNode root = client.get().uri(uri -> uri.path("/query")
                            .queryParam("function", "GLOBAL_QUOTE")
                            .queryParam("symbol", ticker)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve().body(JsonNode.class);
            if (root == null || root.has("Note") || root.has("Information")) {
                throw new ServicoExternoException("Alpha Vantage",
                        "Limite da Alpha Vantage atingido ou chave não configurada. Defina ALPHA_VANTAGE_API_KEY.");
            }
            JsonNode quote = root.path("Global Quote");
            String symbol = quote.path("01. symbol").asText("");
            String price = quote.path("05. price").asText("");
            if (symbol.isBlank() || price.isBlank()) {
                throw new ServicoExternoException("Alpha Vantage", "Ticker americano não encontrado: " + ticker);
            }
            BigDecimal variation = parsePercent(quote.path("10. change percent").asText("0"));
            LocalDateTime date = parseDate(quote.path("07. latest trading day").asText(""));
            return new CotacaoDados(symbol, symbol, "USD", new BigDecimal(price), variation, date, List.of());
        } catch (ServicoExternoException ex) {
            throw ex;
        } catch (RestClientException | NumberFormatException ex) {
            throw new ServicoExternoException("Alpha Vantage", "Não foi possível consultar a cotação americana agora.", ex);
        }
    }

    private static BigDecimal parsePercent(String value) {
        try { return new BigDecimal(value.replace("%", "").trim()); }
        catch (NumberFormatException ex) { return BigDecimal.ZERO; }
    }

    private static LocalDateTime parseDate(String value) {
        try { return LocalDate.parse(value).atTime(18, 0); }
        catch (DateTimeParseException ex) { return LocalDateTime.now(); }
    }
}
