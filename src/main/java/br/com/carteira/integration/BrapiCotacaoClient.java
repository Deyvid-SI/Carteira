package br.com.carteira.integration;

import br.com.carteira.exception.ServicoExternoException;
import br.com.carteira.integration.gateway.CotacaoGateway;
import br.com.carteira.model.Mercado;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class BrapiCotacaoClient implements CotacaoGateway {
    private final RestClient client;
    private final String token;

    public BrapiCotacaoClient(RestClient.Builder builder,
                              @Value("${integracoes.brapi.token:}") String token) {
        this.client = builder.baseUrl("https://brapi.dev").build();
        this.token = token;
    }

    @Override
    public boolean suporta(Mercado mercado) { return mercado == Mercado.BRASIL; }

    @Override
    public CotacaoDados buscar(String ticker) {
        try {
            JsonNode root = client.get()
                    .uri(uri -> uri.path("/api/quote/{ticker}")
                            .queryParam("range", "1mo")
                            .queryParam("interval", "1d")
                            .build(ticker))
                    .headers(headers -> {
                        if (!token.isBlank()) headers.setBearerAuth(token);
                    })
                    .retrieve().body(JsonNode.class);
            JsonNode result = root == null ? null : root.path("results").path(0);
            if (result == null || result.isMissingNode() || result.isEmpty()) {
                throw new ServicoExternoException("brapi", "Ticker brasileiro não encontrado: " + ticker);
            }
            JsonNode data = result.has("data") ? result.path("data") : result;
            BigDecimal preco = decimal(data, "regularMarketPrice");
            if (preco == null) {
                throw new ServicoExternoException("brapi", "A brapi não retornou uma cotação para " + ticker + ".");
            }
            String symbol = text(data, "symbol", text(result, "symbol", ticker));
            String nome = text(data, "longName", text(data, "shortName", symbol));
            String moeda = text(data, "currency", "BRL");
            BigDecimal variacao = decimal(data, "regularMarketChangePercent");
            LocalDateTime dataHora = parseDate(data.path("regularMarketTime"));
            List<PontoHistorico> historico = history(data.path("historicalDataPrice"));
            if (historico.isEmpty() && result != data) {
                historico = history(result.path("historicalDataPrice"));
            }
            return new CotacaoDados(symbol, nome, moeda, preco, variacao, dataHora, historico);
        } catch (ServicoExternoException ex) {
            throw ex;
        } catch (RestClientException ex) {
            throw new ServicoExternoException("brapi", "Não foi possível consultar a cotação brasileira agora.", ex);
        }
    }

    private List<PontoHistorico> history(JsonNode values) {
        List<PontoHistorico> result = new ArrayList<>();
        if (!values.isArray()) return result;
        values.forEach(node -> {
            BigDecimal close = decimal(node, "close");
            if (close != null) result.add(new PontoHistorico(parseDate(node.path("date")), close));
        });
        return result;
    }

    private static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (value.isNumber()) return value.decimalValue();
        if (value.isTextual() && !value.asText().isBlank()) {
            try { return new BigDecimal(value.asText().replace("%", "")); }
            catch (NumberFormatException ignored) { return null; }
        }
        return null;
    }

    private static String text(JsonNode node, String field, String fallback) {
        String value = node.path(field).asText("");
        return value.isBlank() ? fallback : value;
    }

    private static LocalDateTime parseDate(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) return LocalDateTime.now();
        if (node.isNumber()) {
            long epoch = node.asLong();
            if (epoch > 9_999_999_999L) epoch /= 1000;
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(epoch), ZoneId.systemDefault());
        }
        String value = node.asText("");
        try { return OffsetDateTime.parse(value).toLocalDateTime(); }
        catch (DateTimeParseException ignored) {
            try { return LocalDate.parse(value).atStartOfDay(); }
            catch (DateTimeParseException ignoredAgain) { return LocalDateTime.now(); }
        }
    }
}
