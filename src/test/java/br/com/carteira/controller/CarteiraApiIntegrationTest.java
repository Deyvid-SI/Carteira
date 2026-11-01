package br.com.carteira.controller;

import static org.hamcrest.Matchers.endsWith;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.carteira.exception.ServicoExternoException;
import br.com.carteira.integration.BrasilApiClient;
import br.com.carteira.integration.CotacaoGatewaySelector;
import br.com.carteira.integration.ViaCepClient;
import br.com.carteira.integration.gateway.CepGateway.EnderecoDados;
import br.com.carteira.integration.gateway.CotacaoGateway;
import br.com.carteira.integration.gateway.CotacaoGateway.CotacaoDados;
import br.com.carteira.integration.gateway.CotacaoGateway.PontoHistorico;
import br.com.carteira.integration.gateway.CvmGateway.CvmDados;
import br.com.carteira.integration.gateway.EmpresaGateway.EmpresaDados;
import br.com.carteira.model.Mercado;
import br.com.carteira.model.Usuario;
import br.com.carteira.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Contrato JSON da carteira: status, corpo e erros em ProblemDetail. O JWT real é coberto em AuthApiIntegrationTest. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CarteiraApiIntegrationTest {
    private static final String EMAIL = "carteira-api@carteira.test";
    private static final String CNPJ = "02332886000104";
    private static final String CORPO_CORRETORA =
            "{\"cnpj\":\"02.332.886/0001-04\",\"cep\":\"22440-032\",\"numero\":\"153\",\"complemento\":\"Sala 201\"}";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarioRepository;

    @MockitoBean CotacaoGatewaySelector selector;
    @MockitoBean BrasilApiClient brasilApi;
    @MockitoBean ViaCepClient viaCep;

    private CotacaoGateway gateway;

    @BeforeEach
    void setUp() {
        gateway = mock(CotacaoGateway.class);
        when(selector.para(Mercado.BRASIL)).thenReturn(gateway);
        Usuario usuario = new Usuario();
        usuario.setNome("Investidor da API");
        usuario.setEmail(EMAIL);
        usuario.setSenha("senha-nao-utilizada-no-mockmvc");
        usuarioRepository.save(usuario);
    }

    @Test
    @DisplayName("POST /api/acoes valida o ticker e devolve os cálculos da posição")
    void deveCadastrarAcaoComPosicaoInicial() throws Exception {
        when(gateway.buscar("PETR4")).thenReturn(cotacao("40"));

        mvc.perform(json(post("/api/acoes"), "{\"ticker\":\"petr4\",\"mercado\":\"BRASIL\",\"quantidade\":10,\"precoMedio\":30}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.ticker").value("PETR4"))
                .andExpect(jsonPath("$.nomeEmpresa").value("Petrobras"))
                .andExpect(jsonPath("$.mercado").value("BRASIL"))
                .andExpect(jsonPath("$.moeda").value("BRL"))
                .andExpect(jsonPath("$.logoUrl").value(endsWith("/PETR4.SA.png")))
                .andExpect(jsonPath("$.valorInvestido").value(300.0))
                .andExpect(jsonPath("$.valorAtual").value(400.0))
                .andExpect(jsonPath("$.resultado").value(100.0));
    }

    @Test
    @DisplayName("Consultas de ação por lista, id e ticker veem a ação cadastrada")
    void deveConsultarAcaoCadastrada() throws Exception {
        long id = cadastrarAcao();

        mvc.perform(get("/api/acoes").param("page", "0").param("size", "5").with(investidor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].ticker").value("PETR4"))
                .andExpect(jsonPath("$.totalElements").value(1));
        mvc.perform(get("/api/acoes/{id}", id).with(investidor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/acoes/ticker/{ticker}", "petr4").with(investidor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
    }

    @Test
    @DisplayName("PUT atualiza a cotação no provedor")
    void deveAtualizarCotacao() throws Exception {
        long id = cadastrarAcao();
        when(gateway.buscar("PETR4")).thenReturn(cotacao("55"));

        mvc.perform(put("/api/acoes/{id}/atualizar-cotacao", id).with(investidor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.cotacaoAtual").value(55));
    }

    @Test
    @DisplayName("PUT de posição grava o novo saldo e recusa corpo inválido com 400")
    void deveAtualizarPosicao() throws Exception {
        long id = cadastrarAcao();

        mvc.perform(json(put("/api/acoes/{id}/posicao", id), "{\"quantidade\":25,\"precoMedio\":19.75}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidade").value(25))
                .andExpect(jsonPath("$.precoMedio").value(19.75));
        mvc.perform(json(put("/api/acoes/{id}/posicao", id), "{\"quantidade\":0,\"precoMedio\":0}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Dados inválidos"))
                .andExpect(jsonPath("$.erros.quantidade").value("A quantidade deve ser maior que zero"))
                .andExpect(jsonPath("$.erros.precoMedio").value("O preço médio deve ser maior que zero"));
    }

    @Test
    @DisplayName("Ticker repetido responde 422 e ticker inexistente responde 404")
    void deveResponderErrosDeRegraEmProblemDetail() throws Exception {
        cadastrarAcao();

        mvc.perform(json(post("/api/acoes"), "{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("Regra de negócio violada"))
                .andExpect(jsonPath("$.detail").value("Este ticker já está cadastrado na sua carteira."))
                .andExpect(jsonPath("$.instance").value("/api/acoes"))
                .andExpect(jsonPath("$.timestamp").exists());
        mvc.perform(get("/api/acoes/ticker/{ticker}", "XXXX3").with(investidor()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Recurso não encontrado"));
    }

    @Test
    @DisplayName("Falha do provedor responde 502 e identifica o serviço")
    void deveResponder502QuandoProvedorFalhar() throws Exception {
        when(gateway.buscar("PETR4")).thenThrow(new ServicoExternoException("brapi", "Provedor indisponível"));

        mvc.perform(json(post("/api/acoes"), "{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.title").value("Falha em serviço externo"))
                .andExpect(jsonPath("$.servico").value("brapi"))
                .andExpect(jsonPath("$.detail").value("Provedor indisponível"));
    }

    @Test
    @DisplayName("POST /api/negociacoes registra compra e venda e calcula o resultado")
    void deveRegistrarNegociacoes() throws Exception {
        long id = cadastrarAcao();

        mvc.perform(json(post("/api/negociacoes"), negociacao(id, "COMPRA", 10, "30")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.acaoId").value(id))
                .andExpect(jsonPath("$.ticker").value("PETR4"))
                .andExpect(jsonPath("$.mercado").value("BRASIL"))
                .andExpect(jsonPath("$.tipo").value("COMPRA"))
                .andExpect(jsonPath("$.valorBruto").value(300.0))
                .andExpect(jsonPath("$.taxas").value(0.0));
        mvc.perform(json(post("/api/negociacoes"), negociacao(id, "VENDA", 4, "40")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.resultadoRealizado").value(40.0));
        mvc.perform(get("/api/negociacoes").with(investidor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].tipo").value("VENDA"));
    }

    @Test
    @DisplayName("Venda acima do saldo responde 422 e ativo fora da carteira responde 404")
    void deveRecusarNegociacoesInvalidas() throws Exception {
        long id = cadastrarAcao();

        mvc.perform(json(post("/api/negociacoes"), negociacao(id, "VENDA", 1, "40")))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("A quantidade vendida supera o saldo disponível de 0 unidades."));
        mvc.perform(json(post("/api/negociacoes"), negociacao(999_999L, "COMPRA", 1, "40")))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("POST /api/corretoras combina Receita, CVM e CEP no corpo da resposta")
    void deveCadastrarCorretora() throws Exception {
        stubCorretora(true);

        mvc.perform(json(post("/api/corretoras"), CORPO_CORRETORA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.cnpj").value(CNPJ))
                .andExpect(jsonPath("$.razaoSocial").value("XP CORRETORA"))
                .andExpect(jsonPath("$.validadaNaCvm").value(true))
                .andExpect(jsonPath("$.statusCvm").value("EM FUNCIONAMENTO"))
                .andExpect(jsonPath("$.cidade").value("Rio de Janeiro"))
                .andExpect(jsonPath("$.numero").value("153"))
                .andExpect(jsonPath("$.complemento").value("Sala 201"));
    }

    @Test
    @DisplayName("Consultas de corretora por lista, id e CNPJ veem a corretora cadastrada")
    void deveConsultarCorretoraCadastrada() throws Exception {
        stubCorretora(true);
        String corpo = mvc.perform(json(post("/api/corretoras"), CORPO_CORRETORA))
                .andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(corpo, "$.id");

        mvc.perform(get("/api/corretoras").with(investidor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].cnpj").value(CNPJ));
        mvc.perform(get("/api/corretoras/{id}", id).with(investidor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razaoSocial").value("XP CORRETORA"));
        mvc.perform(get("/api/corretoras/cnpj/{cnpj}", CNPJ).with(investidor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/corretoras/{id}", 999_999).with(investidor()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.detail").value("Corretora não encontrada na sua carteira."));
    }

    @Test
    @DisplayName("Corretora sem registro na CVM responde 422 e CNPJ malformado responde 400")
    void deveRecusarCorretorasInvalidas() throws Exception {
        stubCorretora(false);

        mvc.perform(json(post("/api/corretoras"), CORPO_CORRETORA))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("A instituição não consta como corretora autorizada na CVM."));
        mvc.perform(json(post("/api/corretoras"), "{\"cnpj\":\"123\",\"cep\":\"22440032\",\"numero\":\"153\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.cnpj").value("CNPJ inválido"));
    }

    @Test
    @DisplayName("GET /api/dashboard/dados consolida a carteira da conta")
    void deveDevolverDadosDoDashboard() throws Exception {
        cadastrarAcao();

        mvc.perform(get("/api/dashboard/dados").with(investidor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAtivos").value(1))
                .andExpect(jsonPath("$.totalCorretoras").value(0));
    }

    private long cadastrarAcao() throws Exception {
        when(gateway.buscar("PETR4")).thenReturn(cotacao("40"));
        String corpo = mvc.perform(json(post("/api/acoes"), "{\"ticker\":\"PETR4\",\"mercado\":\"BRASIL\"}"))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return ((Number) JsonPath.read(corpo, "$.id")).longValue();
    }

    private void stubCorretora(boolean registrada) {
        when(brasilApi.buscarPorCnpj(CNPJ)).thenReturn(new EmpresaDados(CNPJ, "XP CORRETORA", "XP",
                "contato@xp.test", "2130000000", "22440032", "ATIVA"));
        when(brasilApi.validarCorretora(CNPJ)).thenReturn(new CvmDados(registrada, "XP CORRETORA", "EM FUNCIONAMENTO"));
        when(viaCep.buscarPorCep("22440032")).thenReturn(new EnderecoDados("22440032",
                "Avenida Ataulfo de Paiva", "lado impar", "Leblon", "Rio de Janeiro", "RJ"));
    }

    private static String negociacao(long acaoId, String tipo, int quantidade, String preco) {
        return "{\"acaoId\":%d,\"tipo\":\"%s\",\"quantidade\":%d,\"precoUnitario\":%s,\"dataNegociacao\":\"%s\"}"
                .formatted(acaoId, tipo, quantidade, preco, LocalDateTime.now().minusHours(1).withNano(0));
    }

    private static CotacaoDados cotacao(String preco) {
        return new CotacaoDados("PETR4", "Petrobras", "BRL", new BigDecimal(preco), BigDecimal.ONE, LocalDateTime.now().minusMinutes(5),
                List.of(new PontoHistorico(LocalDateTime.now().minusDays(1), new BigDecimal(preco))));
    }

    private static MockHttpServletRequestBuilder json(MockHttpServletRequestBuilder request, String corpo) {
        return request.with(investidor()).contentType(MediaType.APPLICATION_JSON).content(corpo);
    }

    private static UserRequestPostProcessor investidor() {
        return user(EMAIL).roles("USER");
    }
}
