package br.com.carteira.service;

import br.com.carteira.dto.CadastrarAcaoRequest;
import br.com.carteira.dto.AtualizarPosicaoRequest;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.exception.RecursoNaoEncontradoException;
import br.com.carteira.integration.CotacaoGatewaySelector;
import br.com.carteira.integration.gateway.CotacaoGateway.CotacaoDados;
import br.com.carteira.model.Acao;
import br.com.carteira.model.HistoricoCotacao;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.HistoricoCotacaoRepository;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Locale;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AcaoService {
    private final AcaoRepository repository;
    private final HistoricoCotacaoRepository historicoRepository;
    private final CotacaoGatewaySelector selector;
    private final UsuarioAtualService usuarioAtual;

    public AcaoService(AcaoRepository repository, HistoricoCotacaoRepository historicoRepository,
                       CotacaoGatewaySelector selector, UsuarioAtualService usuarioAtual) {
        this.repository = repository;
        this.historicoRepository = historicoRepository;
        this.selector = selector;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    public Acao cadastrar(CadastrarAcaoRequest request) {
        var usuario = usuarioAtual.obter();
        String ticker = normalize(request.getTicker());
        if (repository.existsByTickerIgnoreCaseAndUsuarioId(ticker, usuario.getId())) {
            throw new RegraNegocioException("Este ticker já está cadastrado na sua carteira.");
        }
        CotacaoDados quote = selector.para(request.getMercado()).buscar(ticker);
        Acao acao = new Acao();
        acao.setUsuario(usuario);
        acao.setTicker(quote.ticker().toUpperCase(Locale.ROOT));
        acao.setNomeEmpresa(quote.nomeEmpresa());
        acao.setMercado(request.getMercado());
        applyQuote(acao, quote);
        var quantidadeInicial = unidades(request.getQuantidade() == null ? BigDecimal.ZERO : request.getQuantidade(), true);
        acao.setQuantidade(quantidadeInicial);
        acao.setPrecoMedio(request.getPrecoMedio() != null ? request.getPrecoMedio()
                : quantidadeInicial.signum() > 0 ? quote.preco() : java.math.BigDecimal.ZERO);
        repository.save(acao);
        saveHistory(acao, quote);
        return acao;
    }

    @Transactional
    public Acao atualizarCotacao(Long id) {
        Acao acao = buscar(id);
        CotacaoDados quote = selector.para(acao.getMercado()).buscar(acao.getTicker());
        applyQuote(acao, quote);
        repository.save(acao);
        historicoRepository.save(new HistoricoCotacao(acao, quote.preco(), quote.dataHora()));
        return acao;
    }

    @Transactional
    public Acao atualizarPosicao(Long id, AtualizarPosicaoRequest request) {
        Acao acao = buscar(id);
        acao.setQuantidade(unidades(request.getQuantidade(), false));
        acao.setPrecoMedio(request.getPrecoMedio());
        return repository.save(acao);
    }

    private void applyQuote(Acao acao, CotacaoDados quote) {
        acao.setMoeda(quote.moeda());
        acao.setCotacaoAtual(quote.preco());
        acao.setVariacaoPercentual(quote.variacaoPercentual());
        acao.setDataHoraCotacao(quote.dataHora() == null ? LocalDateTime.now() : quote.dataHora());
    }

    private void saveHistory(Acao acao, CotacaoDados quote) {
        if (quote.historico() == null || quote.historico().isEmpty()) {
            historicoRepository.save(new HistoricoCotacao(acao, quote.preco(), quote.dataHora()));
            return;
        }
        historicoRepository.saveAll(quote.historico().stream()
                .map(point -> new HistoricoCotacao(acao, point.valor(), point.dataHora())).toList());
    }

    @Transactional(readOnly = true)
    public Page<Acao> listar(Pageable pageable) {
        return repository.findAllByUsuarioId(usuarioAtual.id(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Acao> listarTodos() {
        return repository.findAllByUsuarioIdOrderByTickerAsc(usuarioAtual.id());
    }

    @Transactional(readOnly = true)
    public Acao buscar(Long id) {
        return repository.findByIdAndUsuarioId(id, usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada na sua carteira."));
    }

    @Transactional(readOnly = true)
    public Acao buscarPorTicker(String ticker) {
        return repository.findByTickerIgnoreCaseAndUsuarioId(normalize(ticker), usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada na sua carteira."));
    }

    private BigDecimal unidades(BigDecimal quantidade, boolean permiteZero) {
        if (quantidade == null || (!permiteZero && quantidade.signum() <= 0) || quantidade.signum() < 0
                || quantidade.stripTrailingZeros().scale() > 0) {
            throw new RegraNegocioException("A quantidade deve ser um número inteiro"
                    + (permiteZero ? " não negativo." : " maior que zero."));
        }
        return quantidade.setScale(0, RoundingMode.UNNECESSARY);
    }

    private static String normalize(String ticker) { return ticker.trim().toUpperCase(Locale.ROOT); }
}
