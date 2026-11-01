package br.com.carteira.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import br.com.carteira.dto.CadastrarCorretoraRequest;
import br.com.carteira.exception.RecursoNaoEncontradoException;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.integration.gateway.CepGateway;
import br.com.carteira.integration.gateway.CepGateway.EnderecoDados;
import br.com.carteira.integration.gateway.CvmGateway;
import br.com.carteira.integration.gateway.CvmGateway.CvmDados;
import br.com.carteira.integration.gateway.EmpresaGateway;
import br.com.carteira.integration.gateway.EmpresaGateway.EmpresaDados;
import br.com.carteira.model.Corretora;
import br.com.carteira.repository.CorretoraRepository;
import br.com.carteira.model.Usuario;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

class CorretoraServiceTest {
    private CorretoraRepository repository;
    private EmpresaGateway empresaGateway;
    private CvmGateway cvmGateway;
    private CepGateway cepGateway;
    private CorretoraService service;
    private UsuarioAtualService usuarioAtual;
    private Usuario usuario;

    @BeforeEach
    void setUp() {
        repository = mock(CorretoraRepository.class);
        empresaGateway = mock(EmpresaGateway.class);
        cvmGateway = mock(CvmGateway.class);
        cepGateway = mock(CepGateway.class);
        usuarioAtual = mock(UsuarioAtualService.class);
        usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(7L);
        when(usuarioAtual.obter()).thenReturn(usuario);
        when(usuarioAtual.id()).thenReturn(7L);
        service = new CorretoraService(repository, empresaGateway, cvmGateway, cepGateway, usuarioAtual);
    }

    @Test
    void naoDeveConsultarApiQuandoCnpjForInvalido() {
        CadastrarCorretoraRequest request = request("02.332.886/0001-05");
        assertThatThrownBy(() -> service.cadastrar(request)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("dígitos verificadores");
        verifyNoInteractions(empresaGateway);
    }

    @Test
    void deveImpedirCnpjDuplicado() {
        CadastrarCorretoraRequest request = request("02.332.886/0001-04");
        when(repository.existsByCnpjAndUsuarioId("02332886000104", 7L)).thenReturn(true);
        assertThatThrownBy(() -> service.cadastrar(request)).isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("já está cadastrada");
        verifyNoInteractions(empresaGateway);
    }

    @Test
    void deveCadastrarCorretoraCombinandoAsTresConsultas() {
        when(repository.save(any(Corretora.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(empresaGateway.buscarPorCnpj("02332886000104")).thenReturn(new EmpresaDados("02332886000104",
                "XP CORRETORA", "XP", "contato@xp.test", "2130000000", "22440032", "ATIVA"));
        when(cvmGateway.validarCorretora("02332886000104"))
                .thenReturn(new CvmDados(true, "XP CORRETORA", "EM FUNCIONAMENTO"));
        when(cepGateway.buscarPorCep("22440032")).thenReturn(new EnderecoDados("22440032",
                "Avenida Ataulfo de Paiva", "lado impar", "Leblon", "Rio de Janeiro", "RJ"));
        CadastrarCorretoraRequest request = request("02.332.886/0001-04");
        request.setNumero(" 153 ");
        request.setComplemento("Sala 201");

        Corretora corretora = service.cadastrar(request);

        assertThat(corretora.getUsuario()).isSameAs(usuario);
        assertThat(corretora.getCnpj()).isEqualTo("02332886000104");
        assertThat(corretora.getRazaoSocial()).isEqualTo("XP CORRETORA");
        assertThat(corretora.getNomeFantasia()).isEqualTo("XP");
        assertThat(corretora.getSituacaoCadastral()).isEqualTo("ATIVA");
        assertThat(corretora.isValidadaNaCvm()).isTrue();
        assertThat(corretora.getStatusCvm()).isEqualTo("EM FUNCIONAMENTO");
        assertThat(corretora.getCep()).isEqualTo("22440032");
        assertThat(corretora.getCidade()).isEqualTo("Rio de Janeiro");
        assertThat(corretora.getUf()).isEqualTo("RJ");
        assertThat(corretora.getNumero()).isEqualTo("153");
        assertThat(corretora.getComplemento()).isEqualTo("Sala 201");
    }

    @Test
    void deveRecusarInstituicaoSemRegistroNaCvmSemConsultarOCep() {
        when(empresaGateway.buscarPorCnpj("02332886000104")).thenReturn(new EmpresaDados("02332886000104",
                "EMPRESA COMUM", null, null, null, "22440032", "ATIVA"));
        when(cvmGateway.validarCorretora("02332886000104")).thenReturn(new CvmDados(false, null, "NÃO LOCALIZADA"));

        assertThatThrownBy(() -> service.cadastrar(request("02.332.886/0001-04")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não consta como corretora autorizada");
        verifyNoInteractions(cepGateway);
        verify(repository, never()).save(any());
    }

    @Test
    void deveRecusarCnpjNuloComoInvalido() {
        assertThatThrownBy(() -> service.cadastrar(request(null)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("dígitos verificadores");
    }

    @Test
    void buscarDeveProcurarSomenteNaCarteiraDoUsuario() {
        Corretora corretora = new Corretora();
        when(repository.findByIdAndUsuarioId(1L, 7L)).thenReturn(Optional.of(corretora));
        when(repository.findByIdAndUsuarioId(2L, 7L)).thenReturn(Optional.empty());

        assertThat(service.buscar(1L)).isSameAs(corretora);
        assertThatThrownBy(() -> service.buscar(2L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessage("Corretora não encontrada na sua carteira.");
    }

    @Test
    void buscarPorCnpjDeveConsiderarSomenteOsDigitos() {
        Corretora corretora = new Corretora();
        when(repository.findByCnpjAndUsuarioId("02332886000104", 7L)).thenReturn(Optional.of(corretora));

        assertThat(service.buscarPorCnpj("02.332.886/0001-04")).isSameAs(corretora);
        assertThatThrownBy(() -> service.buscarPorCnpj("11.222.333/0001-81"))
                .isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void listagensDevemFiltrarPeloUsuarioAtual() {
        Corretora corretora = new Corretora();
        Pageable pagina = PageRequest.of(0, 20);
        when(repository.findAllByUsuarioId(7L, pagina)).thenReturn(new PageImpl<>(List.of(corretora)));
        when(repository.findAllByUsuarioIdOrderByRazaoSocialAsc(7L)).thenReturn(List.of(corretora));

        assertThat(service.listar(pagina).getContent()).containsExactly(corretora);
        assertThat(service.listarTodos()).containsExactly(corretora);
    }

    private CadastrarCorretoraRequest request(String cnpj) {
        CadastrarCorretoraRequest request = new CadastrarCorretoraRequest();
        request.setCnpj(cnpj); request.setCep("22440-032"); request.setNumero("153");
        return request;
    }
}
