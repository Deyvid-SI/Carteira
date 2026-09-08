package br.com.carteira.service;

import br.com.carteira.dto.RegistrarNegociacaoRequest;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.model.Acao;
import br.com.carteira.model.Negociacao;
import br.com.carteira.model.TipoNegociacao;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.NegociacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NegociacaoServiceTest {
    private NegociacaoRepository repository;
    private AcaoRepository acaoRepository;
    private NegociacaoService service;
    private Acao acao;

    @BeforeEach
    void setUp() {
        repository = mock(NegociacaoRepository.class);
        acaoRepository = mock(AcaoRepository.class);
        service = new NegociacaoService(repository, acaoRepository);
        acao = new Acao();
        acao.setTicker("PETR4");
        acao.setMoeda("BRL");
        acao.setQuantidade(new BigDecimal("10.0000"));
        acao.setPrecoMedio(new BigDecimal("100.0000"));
        acao.setCotacaoAtual(new BigDecimal("120.0000"));
        when(acaoRepository.buscarParaNegociacao(1L)).thenReturn(Optional.of(acao));
        when(repository.save(any(Negociacao.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveUsarCotacaoAtualNaCompraSemTaxaManual() {
        Negociacao negociacao = service.registrar(request(TipoNegociacao.COMPRA, "5"));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("15.0000");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("106.6667");
        assertThat(negociacao.getPrecoUnitario()).isEqualByComparingTo("120.0000");
        assertThat(negociacao.getValorBruto()).isEqualByComparingTo("600.0000");
        assertThat(negociacao.getTaxas()).isEqualByComparingTo("0.0000");
        assertThat(negociacao.getValorLiquido()).isEqualByComparingTo("600.0000");
        assertThat(negociacao.getResultadoRealizado()).isEqualByComparingTo("0.0000");
    }

    @Test
    void deveUsarCotacaoAtualNaVendaEReduzirSaldo() {
        Negociacao negociacao = service.registrar(request(TipoNegociacao.VENDA, "4"));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("6.0000");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("100.0000");
        assertThat(negociacao.getPrecoUnitario()).isEqualByComparingTo("120.0000");
        assertThat(negociacao.getTaxas()).isEqualByComparingTo("0.0000");
        assertThat(negociacao.getValorLiquido()).isEqualByComparingTo("480.0000");
        assertThat(negociacao.getResultadoRealizado()).isEqualByComparingTo("80.0000");
    }

    @Test
    void deveRecusarVendaAcimaDoSaldoSemPersistir() {
        assertThatThrownBy(() -> service.registrar(request(TipoNegociacao.VENDA, "11")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("supera o saldo disponível");

        verify(repository, never()).save(any());
        verify(acaoRepository, never()).save(any());
    }

    @Test
    void deveRecusarAtivoSemCotacaoValida() {
        acao.setCotacaoAtual(BigDecimal.ZERO);

        assertThatThrownBy(() -> service.registrar(request(TipoNegociacao.COMPRA, "1")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("cotação válida");

        verify(repository, never()).save(any());
    }

    private RegistrarNegociacaoRequest request(TipoNegociacao tipo, String quantidade) {
        RegistrarNegociacaoRequest request = new RegistrarNegociacaoRequest();
        request.setAcaoId(1L);
        request.setTipo(tipo);
        request.setQuantidade(new BigDecimal(quantidade));
        request.setDataNegociacao(LocalDateTime.now().minusMinutes(1));
        return request;
    }
}
