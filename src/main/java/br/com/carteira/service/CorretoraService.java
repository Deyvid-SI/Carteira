package br.com.carteira.service;

import br.com.carteira.dto.CadastrarCorretoraRequest;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.exception.RecursoNaoEncontradoException;
import br.com.carteira.integration.gateway.CepGateway;
import br.com.carteira.integration.gateway.CvmGateway;
import br.com.carteira.integration.gateway.EmpresaGateway;
import br.com.carteira.model.Corretora;
import br.com.carteira.repository.CorretoraRepository;
import br.com.carteira.util.CnpjValidator;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CorretoraService {
    private final CorretoraRepository repository;
    private final EmpresaGateway empresaGateway;
    private final CvmGateway cvmGateway;
    private final CepGateway cepGateway;

    public CorretoraService(CorretoraRepository repository, EmpresaGateway empresaGateway,
                            CvmGateway cvmGateway, CepGateway cepGateway) {
        this.repository = repository;
        this.empresaGateway = empresaGateway;
        this.cvmGateway = cvmGateway;
        this.cepGateway = cepGateway;
    }

    @Transactional
    public Corretora cadastrar(CadastrarCorretoraRequest request) {
        String cnpj = digits(request.getCnpj());
        if (!CnpjValidator.isValid(cnpj)) throw new RegraNegocioException("Os dígitos verificadores do CNPJ são inválidos.");
        if (repository.existsByCnpj(cnpj)) throw new RegraNegocioException("Esta corretora já está cadastrada.");

        var empresa = empresaGateway.buscarPorCnpj(cnpj);
        var cvm = cvmGateway.validarCorretora(cnpj);
        if (!cvm.registrada()) {
            throw new RegraNegocioException("A instituição não consta como corretora autorizada na CVM.");
        }
        String cep = digits(request.getCep());
        var endereco = cepGateway.buscarPorCep(cep);

        Corretora corretora = new Corretora();
        corretora.setCnpj(cnpj);
        corretora.setRazaoSocial(empresa.razaoSocial());
        corretora.setNomeFantasia(empresa.nomeFantasia());
        corretora.setEmail(empresa.email());
        corretora.setTelefone(empresa.telefone());
        corretora.setSituacaoCadastral(empresa.situacaoCadastral());
        corretora.setCep(endereco.cep());
        corretora.setLogradouro(endereco.logradouro());
        corretora.setNumero(request.getNumero().trim());
        corretora.setComplemento(request.getComplemento());
        corretora.setBairro(endereco.bairro());
        corretora.setCidade(endereco.cidade());
        corretora.setUf(endereco.uf());
        corretora.setValidadaNaCvm(true);
        corretora.setStatusCvm(cvm.status());
        return repository.save(corretora);
    }

    @Transactional(readOnly = true)
    public Page<Corretora> listar(Pageable pageable) { return repository.findAll(pageable); }

    @Transactional(readOnly = true)
    public List<Corretora> listarTodos() { return repository.findAll(); }

    @Transactional(readOnly = true)
    public Corretora buscar(Long id) {
        return repository.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Corretora não encontrada."));
    }

    @Transactional(readOnly = true)
    public Corretora buscarPorCnpj(String cnpj) {
        return repository.findByCnpj(digits(cnpj))
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora não encontrada."));
    }

    private static String digits(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }
}
