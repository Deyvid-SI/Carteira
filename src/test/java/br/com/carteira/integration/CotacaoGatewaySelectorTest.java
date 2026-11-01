package br.com.carteira.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.integration.gateway.CotacaoGateway;
import br.com.carteira.model.Mercado;
import java.util.List;
import org.junit.jupiter.api.Test;

class CotacaoGatewaySelectorTest {

    @Test
    void deveEscolherOProvedorDoMercado() {
        CotacaoGateway brasil = mock(CotacaoGateway.class);
        CotacaoGateway eua = mock(CotacaoGateway.class);
        when(brasil.suporta(Mercado.BRASIL)).thenReturn(true);
        when(eua.suporta(Mercado.ESTADOS_UNIDOS)).thenReturn(true);
        CotacaoGatewaySelector selector = new CotacaoGatewaySelector(List.of(brasil, eua));

        assertThat(selector.para(Mercado.BRASIL)).isSameAs(brasil);
        assertThat(selector.para(Mercado.ESTADOS_UNIDOS)).isSameAs(eua);
    }

    @Test
    void deveRecusarMercadoSemProvedor() {
        CotacaoGatewaySelector selector = new CotacaoGatewaySelector(List.of());

        assertThatThrownBy(() -> selector.para(Mercado.BRASIL))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Não existe provedor de cotação para BRASIL");
    }
}
