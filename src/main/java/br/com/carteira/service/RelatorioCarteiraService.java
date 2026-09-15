package br.com.carteira.service;

import br.com.carteira.dto.HistoricoResumo;
import br.com.carteira.dto.ResumoFiscalAno;
import br.com.carteira.model.Negociacao;
import br.com.carteira.model.TipoNegociacao;
import br.com.carteira.repository.NegociacaoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RelatorioCarteiraService {
    private final NegociacaoRepository repository;
    private final UsuarioAtualService usuarioAtual;

    public RelatorioCarteiraService(NegociacaoRepository repository, UsuarioAtualService usuarioAtual) {
        this.repository = repository;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional(readOnly = true)
    public List<Negociacao> listarHistorico() {
        return repository.findAllByAcaoUsuarioIdOrderByDataNegociacaoDescIdDesc(usuarioAtual.id());
    }

    public HistoricoResumo resumirHistorico(List<Negociacao> negociacoes) {
        long compras = negociacoes.stream().filter(n -> n.getTipo() == TipoNegociacao.COMPRA).count();
        long vendas = negociacoes.size() - compras;
        long ativos = negociacoes.stream().map(n -> n.getAcao().getTicker()).distinct().count();
        LocalDateTime primeira = negociacoes.stream().map(Negociacao::getDataNegociacao)
                .min(Comparator.naturalOrder()).orElse(null);
        LocalDateTime ultima = negociacoes.stream().map(Negociacao::getDataNegociacao)
                .max(Comparator.naturalOrder()).orElse(null);

        return new HistoricoResumo(
                negociacoes.size(), compras, vendas, ativos,
                somarPorMoeda(negociacoes, "BRL", false),
                somarPorMoeda(negociacoes, "USD", false),
                somarPorMoeda(negociacoes, "BRL", true),
                somarPorMoeda(negociacoes, "USD", true),
                primeira, ultima);
    }

    public List<ResumoFiscalAno> resumirPorAno(List<Negociacao> negociacoes) {
        var porAno = new TreeMap<Integer, List<Negociacao>>(Comparator.reverseOrder());
        negociacoes.forEach(n -> porAno.computeIfAbsent(n.getDataNegociacao().getYear(), ignored -> new ArrayList<>()).add(n));
        return porAno.entrySet().stream().map(entry -> resumirAno(entry.getKey(), entry.getValue())).toList();
    }

    public List<Integer> listarAnos(List<Negociacao> negociacoes) {
        Set<Integer> anos = new LinkedHashSet<>();
        negociacoes.stream().map(n -> n.getDataNegociacao().getYear())
                .sorted(Comparator.reverseOrder()).forEach(anos::add);
        return new ArrayList<>(anos);
    }

    public List<Negociacao> filtrarAno(List<Negociacao> negociacoes, int ano) {
        return negociacoes.stream().filter(n -> n.getDataNegociacao().getYear() == ano).toList();
    }

    public ResumoFiscalAno resumirAno(int ano, List<Negociacao> negociacoes) {
        List<Negociacao> vendas = negociacoes.stream()
                .filter(n -> n.getTipo() == TipoNegociacao.VENDA).toList();
        long compras = negociacoes.size() - vendas.size();
        return new ResumoFiscalAno(
                ano, negociacoes.size(), compras, vendas.size(),
                somarVendas(vendas, "BRL", false),
                somarVendas(vendas, "USD", false),
                somarVendas(vendas, "BRL", true),
                somarVendas(vendas, "USD", true));
    }

    private BigDecimal somarPorMoeda(List<Negociacao> negociacoes, String moeda, boolean resultado) {
        return money(negociacoes.stream()
                .filter(n -> moeda.equalsIgnoreCase(n.getAcao().getMoeda()))
                .map(n -> resultado ? n.getResultadoRealizado() : n.getValorBruto())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal somarVendas(List<Negociacao> vendas, String moeda, boolean resultado) {
        return money(vendas.stream()
                .filter(n -> moeda.equalsIgnoreCase(n.getAcao().getMoeda()))
                .map(n -> resultado ? n.getResultadoRealizado() : n.getValorBruto())
                .reduce(BigDecimal.ZERO, BigDecimal::add));
    }

    private BigDecimal money(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }
}
