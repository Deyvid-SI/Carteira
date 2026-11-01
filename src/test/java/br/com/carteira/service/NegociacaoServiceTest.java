package br.com.carteira.service;

import br.com.carteira.dto.RegistrarNegociacaoRequest;
import br.com.carteira.exception.RecursoNaoEncontradoException;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.model.Acao;
import br.com.carteira.model.Negociacao;
import br.com.carteira.model.TipoNegociacao;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.NegociacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class NegociacaoServiceTest {
    private NegociacaoRepository repository;
    private AcaoRepository acaoRepository;
    private UsuarioAtualService usuarioAtual;
    private NegociacaoService service;
    private Acao acao;

    @BeforeEach
    void setUp() {
        repository = mock(NegociacaoRepository.class);
        acaoRepository = mock(AcaoRepository.class);
        usuarioAtual = mock(UsuarioAtualService.class);
        when(usuarioAtual.id()).thenReturn(7L);
        service = new NegociacaoService(repository, acaoRepository, usuarioAtual);
        acao = new Acao();
        acao.setTicker("PETR4");
        acao.setMoeda("BRL");
        acao.setQuantidade(new BigDecimal("10.0000"));
        acao.setPrecoMedio(new BigDecimal("100.0000"));
        acao.setCotacaoAtual(new BigDecimal("120.0000"));
        when(acaoRepository.buscarParaNegociacao(1L, 7L)).thenReturn(Optional.of(acao));
        when(repository.save(any(Negociacao.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void deveUsarPrecoInformadoNaCompraSemTaxaManual() {
        Negociacao negociacao = service.registrar(request(TipoNegociacao.COMPRA, "5", "130"));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("15.0000");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("110.0000");
        assertThat(negociacao.getPrecoUnitario()).isEqualByComparingTo("130.0000");
        assertThat(negociacao.getValorBruto()).isEqualByComparingTo("650.0000");
        assertThat(negociacao.getTaxas()).isEqualByComparingTo("0.0000");
        assertThat(negociacao.getValorLiquido()).isEqualByComparingTo("650.0000");
        assertThat(negociacao.getResultadoRealizado()).isEqualByComparingTo("0.0000");
    }

    @Test
    void deveManterPrecoMedioNaVendaMesmoComPrecoExecutadoDiferente() {
        Negociacao negociacao = service.registrar(request(TipoNegociacao.VENDA, "4", "130"));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("6.0000");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("100.0000");
        assertThat(negociacao.getPrecoUnitario()).isEqualByComparingTo("130.0000");
        assertThat(negociacao.getTaxas()).isEqualByComparingTo("0.0000");
        assertThat(negociacao.getValorLiquido()).isEqualByComparingTo("520.0000");
        assertThat(negociacao.getResultadoRealizado()).isEqualByComparingTo("120.0000");
    }

    @Test
    void deveManterPrecoMedioAposVendasComPrecosDiferentes() {
        Negociacao vendaComPrejuizo = service.registrar(request(TipoNegociacao.VENDA, "2", "70"));
        Negociacao vendaComLucro = service.registrar(request(TipoNegociacao.VENDA, "4", "160"));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("4.0000");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("100.0000");
        assertThat(vendaComPrejuizo.getResultadoRealizado()).isEqualByComparingTo("-60.0000");
        assertThat(vendaComLucro.getResultadoRealizado()).isEqualByComparingTo("240.0000");
    }

    @Test
    void deveRecusarVendaAcimaDoSaldoSemPersistir() {
        assertThatThrownBy(() -> service.registrar(request(TipoNegociacao.VENDA, "11", "120")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("supera o saldo disponível");

        verify(repository, never()).save(any());
        verify(acaoRepository, never()).save(any());
    }

    @Test
    void deveRecusarPrecoInformadoIgualAZero() {
        assertThatThrownBy(() -> service.registrar(request(TipoNegociacao.COMPRA, "1", "0")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("preço");

        verify(acaoRepository, never()).buscarParaNegociacao(any(), any());
        verify(repository, never()).save(any());
    }

    @Test
    void deveRecusarQuantidadeFracionada() {
        assertThatThrownBy(() -> service.registrar(request(TipoNegociacao.COMPRA, "1.5", "100")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("número inteiro");

        verify(acaoRepository, never()).buscarParaNegociacao(any(), any());
    }

    @Test
    void vendaDeTodoOSaldoDeveZerarOPrecoMedio() {
        Negociacao negociacao = service.registrar(request(TipoNegociacao.VENDA, "10", "130"));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("0");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("0");
        assertThat(negociacao.getResultadoRealizado()).isEqualByComparingTo("300.0000");
    }

    @Test
    void vendaDoLimiteExatoDoSaldoEhAceitaEUmaUnidadeAcimaNao() {
        assertThatThrownBy(() -> service.registrar(request(TipoNegociacao.VENDA, "11", "100")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A quantidade vendida supera o saldo disponível de 10 unidades.");
        assertThat(service.registrar(request(TipoNegociacao.VENDA, "10", "100")).getQuantidade())
                .isEqualByComparingTo("10");
    }

    @Test
    void compraDeAtivoSemSaldoDefineOPrecoMedioPelaCompra() {
        acao.setQuantidade(BigDecimal.ZERO);
        acao.setPrecoMedio(BigDecimal.ZERO);

        service.registrar(request(TipoNegociacao.COMPRA, "4", "25"));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("4");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("25");
    }

    @Test
    void deveInformarAcaoForaDaCarteira() {
        when(acaoRepository.buscarParaNegociacao(1L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.registrar(request(TipoNegociacao.COMPRA, "1", "10")))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Ação não encontrada na sua carteira.");
    }

    @ParameterizedTest
    @MethodSource("requestsIncompletos")
    void deveRecusarRequestIncompletoSemConsultarOBanco(Consumer<RegistrarNegociacaoRequest> alteracao) {
        RegistrarNegociacaoRequest request = request(TipoNegociacao.COMPRA, "1", "10");
        alteracao.accept(request);

        assertThatThrownBy(() -> service.registrar(request))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Preencha ativo, operação, quantidade e preço corretamente.");
        verify(acaoRepository, never()).buscarParaNegociacao(any(), any());
    }

    static Stream<Consumer<RegistrarNegociacaoRequest>> requestsIncompletos() {
        return Stream.of(
                r -> r.setTipo(null),
                r -> r.setAcaoId(null),
                r -> r.setQuantidade(null),
                r -> r.setQuantidade(BigDecimal.ZERO),
                r -> r.setQuantidade(new BigDecimal("-1")),
                r -> r.setPrecoUnitario(null),
                r -> r.setPrecoUnitario(new BigDecimal("-1")));
    }

    @Test
    void deveRecusarDataFuturaOuAusente() {
        RegistrarNegociacaoRequest futura = request(TipoNegociacao.COMPRA, "1", "10");
        futura.setDataNegociacao(LocalDateTime.now().plusMinutes(5));
        RegistrarNegociacaoRequest semData = request(TipoNegociacao.COMPRA, "1", "10");
        semData.setDataNegociacao(null);

        assertThatThrownBy(() -> service.registrar(futura))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A data da negociação não pode estar no futuro.");
        assertThatThrownBy(() -> service.registrar(semData))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A data da negociação não pode estar no futuro.");
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "2.5"})
    void deveRecusarOperacaoQuandoSaldoGravadoForInvalido(String saldo) {
        acao.setQuantidade(new BigDecimal(saldo));

        assertThatThrownBy(() -> service.registrar(request(TipoNegociacao.COMPRA, "1", "10")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("O saldo da ação deve ser um número inteiro não negativo.");
        verify(repository, never()).save(any());
    }

    @Test
    void listagensDevemFiltrarPeloUsuarioAtual() {
        Negociacao negociacao = new Negociacao();
        Pageable pagina = PageRequest.of(0, 20);
        when(repository.findAllByAcaoUsuarioIdOrderByDataNegociacaoDescIdDesc(7L, pagina))
                .thenReturn(new PageImpl<>(List.of(negociacao)));
        when(repository.findTop200ByAcaoUsuarioIdOrderByDataNegociacaoDescIdDesc(7L)).thenReturn(List.of(negociacao));

        assertThat(service.listar(pagina).getContent()).containsExactly(negociacao);
        assertThat(service.listarRecentes()).containsExactly(negociacao);
    }

    private RegistrarNegociacaoRequest request(TipoNegociacao tipo, String quantidade, String precoUnitario) {
        RegistrarNegociacaoRequest request = new RegistrarNegociacaoRequest();
        request.setAcaoId(1L);
        request.setTipo(tipo);
        request.setQuantidade(new BigDecimal(quantidade));
        request.setPrecoUnitario(new BigDecimal(precoUnitario));
        request.setDataNegociacao(LocalDateTime.now().minusMinutes(1));
        return request;
    }
}
