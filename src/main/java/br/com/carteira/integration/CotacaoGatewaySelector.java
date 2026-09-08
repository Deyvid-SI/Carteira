package br.com.carteira.integration;

import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.integration.gateway.CotacaoGateway;
import br.com.carteira.model.Mercado;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CotacaoGatewaySelector {
    private final List<CotacaoGateway> gateways;

    public CotacaoGatewaySelector(List<CotacaoGateway> gateways) { this.gateways = gateways; }

    public CotacaoGateway para(Mercado mercado) {
        return gateways.stream().filter(g -> g.suporta(mercado)).findFirst()
                .orElseThrow(() -> new RegraNegocioException("Não existe provedor de cotação para " + mercado));
    }
}
