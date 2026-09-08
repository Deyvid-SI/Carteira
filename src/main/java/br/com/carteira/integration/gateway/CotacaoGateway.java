package br.com.carteira.integration.gateway;

import br.com.carteira.model.Mercado;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface CotacaoGateway {
    boolean suporta(Mercado mercado);
    CotacaoDados buscar(String ticker);

    record CotacaoDados(String ticker, String nomeEmpresa, String moeda, BigDecimal preco,
                         BigDecimal variacaoPercentual, LocalDateTime dataHora,
                         List<PontoHistorico> historico) {}

    record PontoHistorico(LocalDateTime dataHora, BigDecimal valor) {}
}
