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
    private final UsuarioAtualService usuarioAtual;

    public CorretoraService(CorretoraRepository repository, EmpresaGateway empresaGateway,
                            CvmGateway cvmGateway, CepGateway cepGateway, UsuarioAtualService usuarioAtual) {
        this.repository = repository;
        this.empresaGateway = empresaGateway;
        this.cvmGateway = cvmGateway;
        this.cepGateway = cepGateway;
        this.usuarioAtual = usuarioAtual;
    }

    @Transactional
    public Corretora cadastrar(CadastrarCorretoraRequest request) {
        var usuario = usuarioAtual.obter();
        String cnpj = digits(request.getCnpj());
        if (!CnpjValidator.isValid(cnpj)) throw new RegraNegocioException("Os dígitos verificadores do CNPJ são inválidos.");
        if (repository.existsByCnpjAndUsuarioId(cnpj, usuario.getId())) {
            throw new RegraNegocioException("Esta corretora já está cadastrada na sua carteira.");
        }

        var empresa = empresaGateway.buscarPorCnpj(cnpj);
        var cvm = cvmGateway.validarCorretora(cnpj);
        if (!cvm.registrada()) {
            throw new RegraNegocioException("A instituição não consta como corretora autorizada na CVM.");
        }
        String cep = digits(request.getCep());
        var endereco = cepGateway.buscarPorCep(cep);

        Corretora corretora = new Corretora();
        corretora.setUsuario(usuario);
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
    public Page<Corretora> listar(Pageable pageable) {
        return repository.findAllByUsuarioId(usuarioAtual.id(), pageable);
    }

    @Transactional(readOnly = true)
    public List<Corretora> listarTodos() {
        return repository.findAllByUsuarioIdOrderByRazaoSocialAsc(usuarioAtual.id());
    }

    @Transactional(readOnly = true)
    public Corretora buscar(Long id) {
        return repository.findByIdAndUsuarioId(id, usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora não encontrada na sua carteira."));
    }

    @Transactional(readOnly = true)
    public Corretora buscarPorCnpj(String cnpj) {
        return repository.findByCnpjAndUsuarioId(digits(cnpj), usuarioAtual.id())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Corretora não encontrada na sua carteira."));
    }

    private static String digits(String value) { return value == null ? "" : value.replaceAll("\\D", ""); }
}
