package br.com.carteira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.carteira.dto.AtualizarPosicaoRequest;
import br.com.carteira.dto.CadastrarAcaoRequest;
import br.com.carteira.exception.RecursoNaoEncontradoException;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.integration.CotacaoGatewaySelector;
import br.com.carteira.integration.gateway.CotacaoGateway;
import br.com.carteira.integration.gateway.CotacaoGateway.CotacaoDados;
import br.com.carteira.integration.gateway.CotacaoGateway.PontoHistorico;
import br.com.carteira.model.Acao;
import br.com.carteira.model.HistoricoCotacao;
import br.com.carteira.model.Mercado;
import br.com.carteira.model.Usuario;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.HistoricoCotacaoRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class AcaoServiceTest {
    private static final LocalDateTime DATA = LocalDateTime.of(2026, 10, 20, 17, 0);

    private AcaoRepository repository;
    private HistoricoCotacaoRepository historicoRepository;
    private CotacaoGateway gateway;
    private AcaoService service;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        repository = mock(AcaoRepository.class);
        historicoRepository = mock(HistoricoCotacaoRepository.class);
        gateway = mock(CotacaoGateway.class);
        CotacaoGatewaySelector selector = mock(CotacaoGatewaySelector.class);
        when(selector.para(any())).thenReturn(gateway);
        UsuarioAtualService usuarioAtual = mock(UsuarioAtualService.class);
        usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(7L);
        when(usuarioAtual.obter()).thenReturn(usuario);
        when(usuarioAtual.id()).thenReturn(7L);
        when(repository.save(any(Acao.class))).thenAnswer(invocation -> invocation.getArgument(0));
        service = new AcaoService(repository, historicoRepository, selector, usuarioAtual);
    }

    @Test
    void deveCadastrarTickerNormalizadoComDadosDoProvedorParaOUsuarioAtual() {
        when(gateway.buscar("PETR4")).thenReturn(cotacao("petr4", "38.50", DATA, List.of()));

        Acao acao = service.cadastrar(request(" petr4 ", "10", null));

        assertThat(acao.getTicker()).isEqualTo("PETR4");
        assertThat(acao.getUsuario()).isSameAs(usuario);
        assertThat(acao.getNomeEmpresa()).isEqualTo("Petrobras");
        assertThat(acao.getMoeda()).isEqualTo("BRL");
        assertThat(acao.getMercado()).isEqualTo(Mercado.BRASIL);
        assertThat(acao.getCotacaoAtual()).isEqualByComparingTo("38.50");
        assertThat(acao.getVariacaoPercentual()).isEqualByComparingTo("1.5");
        assertThat(acao.getDataHoraCotacao()).isEqualTo(DATA);
        verify(repository).save(acao);
    }

    @Test
    void precoMedioInformadoTemPrioridadeSobreACotacao() {
        when(gateway.buscar("PETR4")).thenReturn(cotacao("PETR4", "38.50", DATA, List.of()));

        Acao acao = service.cadastrar(request("PETR4", "10", "30.25"));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("10");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("30.25");
    }

    @Test
    void posicaoComSaldoSemPrecoMedioDevePartirDaCotacao() {
        when(gateway.buscar("PETR4")).thenReturn(cotacao("PETR4", "38.50", DATA, List.of()));

        assertThat(service.cadastrar(request("PETR4", "10", null)).getPrecoMedio()).isEqualByComparingTo("38.50");
    }

    @Test
    void ativoSemSaldoFicaComPrecoMedioZero() {
        when(gateway.buscar("PETR4")).thenReturn(cotacao("PETR4", "38.50", DATA, List.of()));

        Acao acao = service.cadastrar(request("PETR4", "0", null));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("0");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("0");
    }

    @Test
    void quantidadeNaoInformadaEquivaleASaldoZero() {
        when(gateway.buscar("PETR4")).thenReturn(cotacao("PETR4", "38.50", DATA, List.of()));
        CadastrarAcaoRequest request = request("PETR4", "0", null);
        request.setQuantidade(null);

        assertThat(service.cadastrar(request).getQuantidade()).isEqualByComparingTo("0");
    }

    @Test
    void deveRecusarTickerJaCadastradoSemConsultarOProvedor() {
        when(repository.existsByTickerIgnoreCaseAndUsuarioId("PETR4", 7L)).thenReturn(true);

        assertThatThrownBy(() -> service.cadastrar(request("petr4", "0", null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("Este ticker já está cadastrado na sua carteira.");
        verifyNoInteractions(gateway);
        verify(repository, never()).save(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"-1", "1.5"})
    void deveRecusarQuantidadeInicialNegativaOuFracionada(String quantidade) {
        when(gateway.buscar("PETR4")).thenReturn(cotacao("PETR4", "38.50", DATA, List.of()));

        assertThatThrownBy(() -> service.cadastrar(request("PETR4", quantidade, null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A quantidade deve ser um número inteiro não negativo.");
        verify(repository, never()).save(any());
    }

    @Test
    void deveGravarOHistoricoDoProvedorQuandoExistir() {
        var pontos = List.of(new PontoHistorico(DATA.minusDays(2), new BigDecimal("37")),
                new PontoHistorico(DATA.minusDays(1), new BigDecimal("38")));
        when(gateway.buscar("PETR4")).thenReturn(cotacao("PETR4", "38.50", DATA, pontos));

        service.cadastrar(request("PETR4", "0", null));

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<HistoricoCotacao>> salvos = ArgumentCaptor.forClass(List.class);
        verify(historicoRepository).saveAll(salvos.capture());
        assertThat(salvos.getValue()).extracting(HistoricoCotacao::getValor)
                .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
                .containsExactly(new BigDecimal("37"), new BigDecimal("38"));
    }

    @Test
    void semHistoricoDoProvedorGravaACotacaoAtualComoPrimeiroPonto() {
        when(gateway.buscar("PETR4")).thenReturn(cotacao("PETR4", "38.50", DATA, List.of()));
        service.cadastrar(request("PETR4", "0", null));
        verificarPontoUnico("38.50");
    }

    @Test
    void historicoNuloTambemGravaACotacaoAtual() {
        when(gateway.buscar("PETR4")).thenReturn(cotacao("PETR4", "38.50", DATA, null));
        service.cadastrar(request("PETR4", "0", null));
        verificarPontoUnico("38.50");
    }

    @Test
    void atualizarCotacaoDeveTrocarOPrecoEAcrescentarAoHistorico() {
        Acao acao = acao();
        when(repository.findByIdAndUsuarioId(1L, 7L)).thenReturn(Optional.of(acao));
        when(gateway.buscar("PETR4")).thenReturn(cotacao("PETR4", "41.00", DATA, List.of()));

        Acao atualizada = service.atualizarCotacao(1L);

        assertThat(atualizada.getCotacaoAtual()).isEqualByComparingTo("41.00");
        assertThat(atualizada.getDataHoraCotacao()).isEqualTo(DATA);
        verificarPontoUnico("41.00");
    }

    @Test
    void cotacaoSemDataUsaOMomentoAtual() {
        Acao acao = acao();
        when(repository.findByIdAndUsuarioId(1L, 7L)).thenReturn(Optional.of(acao));
        when(gateway.buscar("PETR4")).thenReturn(cotacao("PETR4", "41.00", null, List.of()));

        assertThat(service.atualizarCotacao(1L).getDataHoraCotacao())
                .isCloseTo(LocalDateTime.now(), within(1, ChronoUnit.MINUTES));
    }

    @Test
    void atualizarPosicaoDeveGravarQuantidadeEPrecoMedio() {
        when(repository.findByIdAndUsuarioId(1L, 7L)).thenReturn(Optional.of(acao()));

        Acao atualizada = service.atualizarPosicao(1L, posicao("25", "19.75"));

        assertThat(atualizada.getQuantidade()).isEqualByComparingTo("25");
        assertThat(atualizada.getPrecoMedio()).isEqualByComparingTo("19.75");
    }

    @ParameterizedTest
    @ValueSource(strings = {"0", "-3", "2.5"})
    void atualizarPosicaoDeveExigirInteiroMaiorQueZero(String quantidade) {
        when(repository.findByIdAndUsuarioId(1L, 7L)).thenReturn(Optional.of(acao()));

        assertThatThrownBy(() -> service.atualizarPosicao(1L, posicao(quantidade, "19.75")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage("A quantidade deve ser um número inteiro maior que zero.");
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarPosicaoDeveRecusarQuantidadeNula() {
        when(repository.findByIdAndUsuarioId(1L, 7L)).thenReturn(Optional.of(acao()));
        AtualizarPosicaoRequest request = posicao("1", "19.75");
        request.setQuantidade(null);

        assertThatThrownBy(() -> service.atualizarPosicao(1L, request)).isInstanceOf(RegraNegocioException.class);
    }

    @Test
    void buscarDeveConsultarSomenteNaCarteiraDoUsuario() {
        when(repository.findByIdAndUsuarioId(1L, 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscar(1L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Ação não encontrada na sua carteira.");
    }

    @Test
    void buscarPorTickerDeveNormalizarOTexto() {
        Acao acao = acao();
        when(repository.findByTickerIgnoreCaseAndUsuarioId("PETR4", 7L)).thenReturn(Optional.of(acao));

        assertThat(service.buscarPorTicker(" petr4 ")).isSameAs(acao);
    }

    @Test
    void buscarPorTickerInexistenteDeveFalhar() {
        when(repository.findByTickerIgnoreCaseAndUsuarioId("XXXX3", 7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.buscarPorTicker("XXXX3")).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listagensDevemFiltrarPeloUsuarioAtual() {
        Pageable pagina = PageRequest.of(0, 20);
        Acao acao = acao();
        when(repository.findAllByUsuarioId(7L, pagina)).thenReturn(new PageImpl<>(List.of(acao)));
        when(repository.findAllByUsuarioIdOrderByTickerAsc(7L)).thenReturn(List.of(acao));

        assertThat(service.listar(pagina).getContent()).containsExactly(acao);
        assertThat(service.listarTodos()).containsExactly(acao);
    }

    private void verificarPontoUnico(String valorEsperado) {
        ArgumentCaptor<HistoricoCotacao> ponto = ArgumentCaptor.forClass(HistoricoCotacao.class);
        verify(historicoRepository).save(ponto.capture());
        assertThat(ponto.getValue().getValor()).isEqualByComparingTo(valorEsperado);
    }

    private static CotacaoDados cotacao(String ticker, String preco, LocalDateTime data, List<PontoHistorico> historico) {
        return new CotacaoDados(ticker, "Petrobras", "BRL", new BigDecimal(preco), new BigDecimal("1.5"), data, historico);
    }

    private static CadastrarAcaoRequest request(String ticker, String quantidade, String precoMedio) {
        CadastrarAcaoRequest request = new CadastrarAcaoRequest();
        request.setTicker(ticker);
        request.setMercado(Mercado.BRASIL);
        request.setQuantidade(new BigDecimal(quantidade));
        request.setPrecoMedio(precoMedio == null ? null : new BigDecimal(precoMedio));
        return request;
    }

    private static AtualizarPosicaoRequest posicao(String quantidade, String precoMedio) {
        AtualizarPosicaoRequest request = new AtualizarPosicaoRequest();
        request.setQuantidade(new BigDecimal(quantidade));
        request.setPrecoMedio(new BigDecimal(precoMedio));
        return request;
    }

    private static Acao acao() {
        Acao acao = new Acao();
        acao.setTicker("PETR4");
        acao.setMercado(Mercado.BRASIL);
        acao.setCotacaoAtual(new BigDecimal("38.50"));
        return acao;
    }
}
