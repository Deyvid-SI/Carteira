package br.com.carteira.service;

import br.com.carteira.dto.DashboardResponse;
import br.com.carteira.dto.DashboardResponse.*;
import br.com.carteira.model.Acao;
import br.com.carteira.model.HistoricoCotacao;
import br.com.carteira.model.Mercado;
import br.com.carteira.model.Negociacao;
import br.com.carteira.model.TipoNegociacao;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.CorretoraRepository;
import br.com.carteira.repository.HistoricoCotacaoRepository;
import br.com.carteira.repository.NegociacaoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private static final DateTimeFormatter LABEL = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter LABEL_NEGOCIACAO = DateTimeFormatter.ofPattern("dd/MM HH:mm");
    private final AcaoRepository acaoRepository;
    private final CorretoraRepository corretoraRepository;
    private final HistoricoCotacaoRepository historicoRepository;
    private final NegociacaoRepository negociacaoRepository;

    public DashboardService(AcaoRepository acaoRepository, CorretoraRepository corretoraRepository,
                            HistoricoCotacaoRepository historicoRepository,
                            NegociacaoRepository negociacaoRepository) {
        this.acaoRepository = acaoRepository;
        this.corretoraRepository = corretoraRepository;
        this.historicoRepository = historicoRepository;
        this.negociacaoRepository = negociacaoRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse dados() {
        List<Acao> acoes = acaoRepository.findAll();
        long brasil = acoes.stream().filter(a -> a.getMercado() == Mercado.BRASIL).count();
        long usa = acoes.size() - brasil;

        Map<String, Long> distribuicao = new LinkedHashMap<>();
        distribuicao.put("Brasil", brasil);
        distribuicao.put("Estados Unidos", usa);

        Map<String, Map<String, BigDecimal>> alocacao = new LinkedHashMap<>();
        alocacao.put("BRL", new LinkedHashMap<>());
        alocacao.put("USD", new LinkedHashMap<>());
        acoes.forEach(a -> alocacao.computeIfAbsent(a.getMoeda(), ignored -> new LinkedHashMap<>())
                .put(a.getTicker(), a.getValorAtual()));

        List<ResumoMoeda> resumos = alocacao.keySet().stream()
                .map(moeda -> resumo(moeda, acoes)).toList();
        List<SerieCotacao> series = acoes.stream().limit(8).map(this::serie).toList();
        List<SerieDesempenho> desempenhos = acoes.stream().limit(8).map(this::desempenho).toList();
        List<Negociacao> negociacoes = negociacaoRepository.findAllByOrderByDataNegociacaoAscIdAsc();
        List<SeriePrecoMedio> precosMedios = acoes.stream().limit(8)
                .map(a -> precoMedioAcumulado(a, negociacoes)).toList();
        List<AtivoResumo> ativos = acoes.stream()
                .sorted(Comparator.comparing(Acao::getValorAtual).reversed())
                .map(a -> new AtivoResumo(a.getTicker(), a.getNomeEmpresa(), a.getMercado().getDescricao(),
                        a.getMoeda(), a.getCotacaoAtual(), a.getVariacaoPercentual(), a.getQuantidade(),
                        a.getPrecoMedio(), a.getValorInvestido(), a.getValorAtual(), a.getResultado(),
                        a.getRentabilidade(), a.getResultadoDia(), a.getDataHoraCotacao()))
                .toList();
        LocalDateTime ultima = acoes.stream().map(Acao::getDataHoraCotacao)
                .filter(Objects::nonNull).max(LocalDateTime::compareTo).orElse(null);
        return new DashboardResponse(acoes.size(), corretoraRepository.count(), brasil, usa, ultima,
                resumos, distribuicao, alocacao, series, desempenhos, precosMedios, ativos);
    }

    private ResumoMoeda resumo(String moeda, List<Acao> acoes) {
        List<Acao> mesmaMoeda = acoes.stream().filter(a -> moeda.equals(a.getMoeda())).toList();
        BigDecimal investido = sum(mesmaMoeda, Valor.INVESTIDO);
        BigDecimal atual = sum(mesmaMoeda, Valor.ATUAL);
        BigDecimal resultado = atual.subtract(investido).setScale(4, RoundingMode.HALF_UP);
        BigDecimal rentabilidade = investido.signum() == 0 ? BigDecimal.ZERO
                : resultado.multiply(BigDecimal.valueOf(100)).divide(investido, 4, RoundingMode.HALF_UP);
        return new ResumoMoeda(moeda, investido, atual, resultado, rentabilidade,
                sum(mesmaMoeda, Valor.DIA));
    }

    private BigDecimal sum(List<Acao> acoes, Valor valor) {
        return acoes.stream().map(a -> switch (valor) {
            case INVESTIDO -> a.getValorInvestido();
            case ATUAL -> a.getValorAtual();
            case DIA -> a.getResultadoDia();
        }).reduce(BigDecimal.ZERO, BigDecimal::add).setScale(4, RoundingMode.HALF_UP);
    }

    private SerieCotacao serie(Acao acao) {
        List<HistoricoCotacao> history = history(acao);
        return new SerieCotacao(acao.getTicker(), acao.getMoeda(), labels(history),
                history.stream().map(HistoricoCotacao::getValor).toList());
    }

    private SerieDesempenho desempenho(Acao acao) {
        List<HistoricoCotacao> history = history(acao);
        if (history.isEmpty()) return new SerieDesempenho(acao.getTicker(), List.of(), List.of());
        BigDecimal inicio = history.get(0).getValor();
        List<BigDecimal> percentuais = history.stream().map(h -> inicio.signum() == 0 ? BigDecimal.ZERO
                : h.getValor().subtract(inicio).multiply(BigDecimal.valueOf(100))
                        .divide(inicio, 4, RoundingMode.HALF_UP)).toList();
        return new SerieDesempenho(acao.getTicker(), labels(history), percentuais);
    }

    private List<HistoricoCotacao> history(Acao acao) {
        List<HistoricoCotacao> history = new ArrayList<>(
                historicoRepository.findTop30ByAcaoIdOrderByDataHoraDesc(acao.getId()));
        Collections.reverse(history);
        return history;
    }

    private List<String> labels(List<HistoricoCotacao> history) {
        return history.stream().map(h -> h.getDataHora().format(LABEL)).toList();
    }

    private SeriePrecoMedio precoMedioAcumulado(Acao acao, List<Negociacao> todas) {
        List<Negociacao> operacoes = todas.stream().filter(n -> mesmaAcao(acao, n.getAcao())).toList();
        List<String> labels = new ArrayList<>();
        List<BigDecimal> valores = new ArrayList<>();

        BigDecimal quantidade = scale(acao.getQuantidade());
        BigDecimal custo = money(acao.getPrecoMedio().multiply(quantidade));

        List<Negociacao> reversas = new ArrayList<>(operacoes);
        Collections.reverse(reversas);
        for (Negociacao negociacao : reversas) {
            if (negociacao.getTipo() == TipoNegociacao.COMPRA) {
                quantidade = scale(quantidade.subtract(negociacao.getQuantidade()));
                custo = money(custo.subtract(negociacao.getValorLiquido()));
            } else {
                BigDecimal custoVendido = negociacao.getValorLiquido()
                        .subtract(negociacao.getResultadoRealizado());
                quantidade = scale(quantidade.add(negociacao.getQuantidade()));
                custo = money(custo.add(custoVendido));
            }
        }
        quantidade = nonNegative(quantidade);
        custo = nonNegative(custo);

        if (quantidade.signum() > 0) {
            labels.add("Inicial");
            valores.add(media(custo, quantidade));
        }

        for (Negociacao negociacao : operacoes) {
            if (negociacao.getTipo() == TipoNegociacao.COMPRA) {
                quantidade = scale(quantidade.add(negociacao.getQuantidade()));
                custo = money(custo.add(negociacao.getValorLiquido()));
            } else {
                BigDecimal custoVendido = negociacao.getValorLiquido()
                        .subtract(negociacao.getResultadoRealizado());
                quantidade = scale(quantidade.subtract(negociacao.getQuantidade()));
                custo = money(custo.subtract(custoVendido));
            }
            quantidade = nonNegative(quantidade);
            custo = nonNegative(custo);
            labels.add(negociacao.getDataNegociacao().format(LABEL_NEGOCIACAO));
            valores.add(media(custo, quantidade));
        }

        if (labels.isEmpty()) {
            labels.add("Atual");
            valores.add(scale(acao.getPrecoMedio()));
        }
        return new SeriePrecoMedio(acao.getTicker(), acao.getMoeda(), labels, valores);
    }

    private boolean mesmaAcao(Acao primeira, Acao segunda) {
        if (primeira.getId() != null && segunda.getId() != null) {
            return Objects.equals(primeira.getId(), segunda.getId());
        }
        return Objects.equals(primeira.getTicker(), segunda.getTicker());
    }

    private BigDecimal media(BigDecimal custo, BigDecimal quantidade) {
        return quantidade.signum() == 0 ? BigDecimal.ZERO.setScale(4)
                : custo.divide(quantidade, 4, RoundingMode.HALF_UP);
    }

    private BigDecimal nonNegative(BigDecimal value) {
        return value.signum() < 0 ? BigDecimal.ZERO.setScale(4) : value;
    }

    private BigDecimal scale(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private enum Valor { INVESTIDO, ATUAL, DIA }
}
