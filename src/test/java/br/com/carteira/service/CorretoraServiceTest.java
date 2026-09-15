package br.com.carteira.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

import br.com.carteira.dto.CadastrarCorretoraRequest;
import br.com.carteira.exception.RegraNegocioException;
import br.com.carteira.integration.gateway.CepGateway;
import br.com.carteira.integration.gateway.CvmGateway;
import br.com.carteira.integration.gateway.EmpresaGateway;
import br.com.carteira.repository.CorretoraRepository;
import br.com.carteira.model.Usuario;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CorretoraServiceTest {
    private CorretoraRepository repository;
    private EmpresaGateway empresaGateway;
    private CorretoraService service;
    private UsuarioAtualService usuarioAtual;

    @BeforeEach
    void setUp() {
        repository = mock(CorretoraRepository.class);
        empresaGateway = mock(EmpresaGateway.class);
        usuarioAtual = mock(UsuarioAtualService.class);
        Usuario usuario = mock(Usuario.class);
        when(usuario.getId()).thenReturn(7L);
        when(usuarioAtual.obter()).thenReturn(usuario);
        service = new CorretoraService(repository, empresaGateway, mock(CvmGateway.class), mock(CepGateway.class), usuarioAtual);
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

    private CadastrarCorretoraRequest request(String cnpj) {
        CadastrarCorretoraRequest request = new CadastrarCorretoraRequest();
        request.setCnpj(cnpj); request.setCep("22440-032"); request.setNumero("153");
        return request;
    }
}
