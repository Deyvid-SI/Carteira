package br.com.carteira.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public record DashboardResponse(
        long totalAtivos,
        long totalCorretoras,
        long ativosBrasil,
        long ativosEstadosUnidos,
        LocalDateTime ultimaAtualizacao,
        List<ResumoMoeda> resumosPorMoeda,
        Map<String, Long> distribuicaoMercados,
        Map<String, Map<String, BigDecimal>> alocacaoPorMoeda,
        List<SerieCotacao> series,
        List<SerieDesempenho> desempenhos,
        List<SeriePrecoMedio> precosMediosAcumulados,
        List<AtivoResumo> ativos) {

    public record ResumoMoeda(String moeda, BigDecimal valorInvestido, BigDecimal valorAtual,
                              BigDecimal resultado, BigDecimal rentabilidade, BigDecimal resultadoDia) {}

    public record SerieCotacao(String ticker, String moeda, List<String> labels, List<BigDecimal> valores) {}

    public record SerieDesempenho(String ticker, List<String> labels, List<BigDecimal> percentuais) {}

    public record SeriePrecoMedio(String ticker, String moeda, List<String> labels,
                                  List<BigDecimal> valores) {}

    public record AtivoResumo(String ticker, String nome, String logoUrl, String mercado, String moeda,
                              BigDecimal cotacao, BigDecimal variacao, BigDecimal quantidade,
                              BigDecimal precoMedio, BigDecimal valorInvestido, BigDecimal valorAtual,
                              BigDecimal resultado, BigDecimal rentabilidade, BigDecimal resultadoDia,
                              LocalDateTime atualizadoEm) {}
}
