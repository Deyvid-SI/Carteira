package br.com.carteira.service;

import br.com.carteira.dto.RegistrarNegociacaoRequest;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.exception.RecursoNaoEncontradoException;
import br.com.carteira.model.Acao;
import br.com.carteira.model.Negociacao;
import br.com.carteira.model.TipoNegociacao;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.NegociacaoRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NegociacaoService {
    private final NegociacaoRepository repository;
    private final AcaoRepository acaoRepository;

    public NegociacaoService(NegociacaoRepository repository, AcaoRepository acaoRepository) {
        this.repository = repository;
        this.acaoRepository = acaoRepository;
    }

    @Transactional
    public Negociacao registrar(RegistrarNegociacaoRequest request) {
        validarRequest(request);
        Acao acao = acaoRepository.buscarParaNegociacao(request.getAcaoId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada."));

        BigDecimal quantidade = scale(request.getQuantidade());
        BigDecimal preco = cotacaoAtual(acao);
        BigDecimal taxas = money(BigDecimal.ZERO);
        BigDecimal valorBruto = money(quantidade.multiply(preco));
        BigDecimal valorLiquido;
        BigDecimal resultadoRealizado = money(BigDecimal.ZERO);

        if (request.getTipo() == TipoNegociacao.COMPRA) {
            valorLiquido = money(valorBruto.add(taxas));
            aplicarCompra(acao, quantidade, valorLiquido);
        } else {
            valorLiquido = money(valorBruto.subtract(taxas));
            resultadoRealizado = aplicarVenda(acao, quantidade, valorLiquido);
        }

        acaoRepository.save(acao);
        Negociacao negociacao = new Negociacao();
        negociacao.setAcao(acao);
        negociacao.setTipo(request.getTipo());
        negociacao.setQuantidade(quantidade);
        negociacao.setPrecoUnitario(preco);
        negociacao.setTaxas(taxas);
        negociacao.setValorBruto(valorBruto);
        negociacao.setValorLiquido(valorLiquido);
        negociacao.setResultadoRealizado(resultadoRealizado);
        negociacao.setDataNegociacao(request.getDataNegociacao());
        return repository.save(negociacao);
    }

    private void aplicarCompra(Acao acao, BigDecimal quantidadeComprada, BigDecimal custoTotalCompra) {
        BigDecimal quantidadeAtual = scale(acao.getQuantidade());
        BigDecimal novaQuantidade = scale(quantidadeAtual.add(quantidadeComprada));
        BigDecimal custoAtual = acao.getPrecoMedio().multiply(quantidadeAtual);
        BigDecimal novoPrecoMedio = custoAtual.add(custoTotalCompra)
                .divide(novaQuantidade, 4, RoundingMode.HALF_UP);
        acao.setQuantidade(novaQuantidade);
        acao.setPrecoMedio(novoPrecoMedio);
    }

    private BigDecimal aplicarVenda(Acao acao, BigDecimal quantidadeVendida, BigDecimal valorRecebido) {
        BigDecimal quantidadeAtual = scale(acao.getQuantidade());
        if (quantidadeVendida.compareTo(quantidadeAtual) > 0) {
            throw new RegraNegocioException("A quantidade vendida supera o saldo disponível de "
                    + quantidadeAtual.stripTrailingZeros().toPlainString() + " unidades.");
        }
        BigDecimal custoDaVenda = acao.getPrecoMedio().multiply(quantidadeVendida);
        BigDecimal resultado = money(valorRecebido.subtract(custoDaVenda));
        BigDecimal saldo = scale(quantidadeAtual.subtract(quantidadeVendida));
        acao.setQuantidade(saldo);
        if (saldo.signum() == 0) acao.setPrecoMedio(money(BigDecimal.ZERO));
        return resultado;
    }

    private void validarRequest(RegistrarNegociacaoRequest request) {
        if (request.getTipo() == null || request.getAcaoId() == null
                || request.getQuantidade() == null || request.getQuantidade().signum() <= 0) {
            throw new RegraNegocioException("Preencha ativo, operação e quantidade corretamente.");
        }
        if (request.getDataNegociacao() == null || request.getDataNegociacao().isAfter(LocalDateTime.now())) {
            throw new RegraNegocioException("A data da negociação não pode estar no futuro.");
        }
    }

    @Transactional(readOnly = true)
    public Page<Negociacao> listar(Pageable pageable) {
        return repository.findAllByOrderByDataNegociacaoDescIdDesc(pageable);
    }

    @Transactional(readOnly = true)
    public List<Negociacao> listarRecentes() {
        return repository.findTop200ByOrderByDataNegociacaoDescIdDesc();
    }

    private BigDecimal cotacaoAtual(Acao acao) {
        BigDecimal cotacao = acao.getCotacaoAtual();
        if (cotacao == null || cotacao.signum() <= 0) {
            throw new RegraNegocioException("O ativo não possui uma cotação válida. Atualize a cotação antes de negociar.");
        }
        return scale(cotacao);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
