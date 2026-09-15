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
    private final UsuarioAtualService usuarioAtual;

    public NegociacaoService(NegociacaoRepository repository, AcaoRepository acaoRepository,
                             UsuarioAtualService usuarioAtual) {
        this.repository = repository;
        this.acaoRepository = acaoRepository;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    public Negociacao registrar(RegistrarNegociacaoRequest request) {
        validarRequest(request);
        Acao acao = acaoRepository.buscarParaNegociacao(request.getAcaoId(), usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Ação não encontrada na sua carteira."));

        BigDecimal quantidade = unidades(request.getQuantidade());
        BigDecimal preco = scale(request.getPrecoUnitario());
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
        BigDecimal quantidadeAtual = unidadesNaoNegativas(acao.getQuantidade());
        BigDecimal novaQuantidade = unidadesNaoNegativas(quantidadeAtual.add(quantidadeComprada));
        BigDecimal custoAtual = acao.getPrecoMedio().multiply(quantidadeAtual);
        BigDecimal novoPrecoMedio = custoAtual.add(custoTotalCompra)
                .divide(novaQuantidade, 4, RoundingMode.HALF_UP);
        acao.setQuantidade(novaQuantidade);
        acao.setPrecoMedio(novoPrecoMedio);
    }

    private BigDecimal aplicarVenda(Acao acao, BigDecimal quantidadeVendida, BigDecimal valorRecebido) {
        BigDecimal quantidadeAtual = unidadesNaoNegativas(acao.getQuantidade());
        BigDecimal precoMedioAtual = scale(acao.getPrecoMedio());
        if (quantidadeVendida.compareTo(quantidadeAtual) > 0) {
            throw new RegraNegocioException("A quantidade vendida supera o saldo disponível de "
                    + quantidadeAtual.stripTrailingZeros().toPlainString() + " unidades.");
        }
        BigDecimal custoDaVenda = precoMedioAtual.multiply(quantidadeVendida);
        BigDecimal resultado = money(valorRecebido.subtract(custoDaVenda));
        BigDecimal saldo = unidadesNaoNegativas(quantidadeAtual.subtract(quantidadeVendida));
        acao.setQuantidade(saldo);
        acao.setPrecoMedio(saldo.signum() == 0 ? money(BigDecimal.ZERO) : precoMedioAtual);
        return resultado;
    }

    private void validarRequest(RegistrarNegociacaoRequest request) {
        if (request.getTipo() == null || request.getAcaoId() == null
                || request.getQuantidade() == null || request.getQuantidade().signum() <= 0
                || request.getPrecoUnitario() == null || request.getPrecoUnitario().signum() <= 0) {
            throw new RegraNegocioException("Preencha ativo, operação, quantidade e preço corretamente.");
        }
        if (request.getQuantidade().stripTrailingZeros().scale() > 0) {
            throw new RegraNegocioException("A quantidade deve ser um número inteiro.");
        }
        if (request.getDataNegociacao() == null || request.getDataNegociacao().isAfter(LocalDateTime.now())) {
            throw new RegraNegocioException("A data da negociação não pode estar no futuro.");
        }
    }

    @Transactional(readOnly = true)
    public Page<Negociacao> listar(Pageable pageable) {
        return repository.findAllByAcaoUsuarioIdOrderByDataNegociacaoDescIdDesc(usuarioAtual.id(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Negociacao> listarRecentes() {
        return repository.findTop200ByAcaoUsuarioIdOrderByDataNegociacaoDescIdDesc(usuarioAtual.id());
    }

    private static BigDecimal unidades(BigDecimal value) {
        return value.setScale(0, RoundingMode.UNNECESSARY);
    }

    private static BigDecimal unidadesNaoNegativas(BigDecimal value) {
        if (value.signum() < 0 || value.stripTrailingZeros().scale() > 0) {
            throw new RegraNegocioException("O saldo da ação deve ser um número inteiro não negativo.");
        }
        return unidades(value);
    }

    private static BigDecimal scale(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }

    private static BigDecimal money(BigDecimal value) {
        return value.setScale(4, RoundingMode.HALF_UP);
    }
}
