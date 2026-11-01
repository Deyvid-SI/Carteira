package br.com.carteira.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.com.carteira.dto.RegistrarNegociacaoRequest;
import br.com.carteira.exception.ServicoExternoException;
import br.com.carteira.integration.BrasilApiClient;
import br.com.carteira.integration.CotacaoGatewaySelector;
import br.com.carteira.integration.ViaCepClient;
import br.com.carteira.integration.gateway.CepGateway.EnderecoDados;
import br.com.carteira.integration.gateway.CotacaoGateway;
import br.com.carteira.integration.gateway.CotacaoGateway.CotacaoDados;
import br.com.carteira.integration.gateway.CvmGateway.CvmDados;
import br.com.carteira.integration.gateway.EmpresaGateway.EmpresaDados;
import br.com.carteira.model.Acao;
import br.com.carteira.model.Corretora;
import br.com.carteira.model.Mercado;
import br.com.carteira.model.Usuario;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.CorretoraRepository;
import br.com.carteira.repository.HistoricoCotacaoRepository;
import br.com.carteira.repository.NegociacaoRepository;
import br.com.carteira.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Telas de gestão com MVC, validação, CSRF e banco reais. Só as APIs externas são dublês. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class GestaoWebIntegrationTest {
    private static final String EMAIL = "gestao-web@carteira.test";
    private static final String CNPJ = "02332886000104";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired AcaoRepository acaoRepository;
    @Autowired CorretoraRepository corretoraRepository;
    @Autowired NegociacaoRepository negociacaoRepository;
    @Autowired HistoricoCotacaoRepository historicoRepository;

    @MockitoBean CotacaoGatewaySelector selector;
    @MockitoBean BrasilApiClient brasilApi;
    @MockitoBean ViaCepClient viaCep;

    private CotacaoGateway gateway;
    private Usuario conta;

    @BeforeEach
    void setUp() {
        gateway = mock(CotacaoGateway.class);
        when(selector.para(Mercado.BRASIL)).thenReturn(gateway);
        Usuario usuario = new Usuario();
        usuario.setNome("Investidor do portal");
        usuario.setEmail(EMAIL);
        usuario.setSenha("senha-nao-utilizada-no-mockmvc");
        conta = usuarioRepository.save(usuario);
    }

    @Test
    @DisplayName("Ação validada no provedor é cadastrada na carteira da conta")
    void deveCadastrarAcaoValidada() throws Exception {
        when(gateway.buscar("PETR4")).thenReturn(cotacao("38.50"));

        mvc.perform(post("/gestao/acoes").with(investidor()).with(csrf())
                        .param("ticker", "petr4").param("mercado", "BRASIL"))
                .andExpect(redirectedUrl("/gestao/acoes"))
                .andExpect(flash().attribute("sucesso", "Ticker validado e cadastrado com sucesso."));

        Acao salva = acaoRepository.findByTickerIgnoreCaseAndUsuarioId("PETR4", conta.getId()).orElseThrow();
        assertThat(salva.getCotacaoAtual()).isEqualByComparingTo("38.50");
        mvc.perform(get("/gestao/acoes").with(investidor()))
                .andExpect(status().isOk())
                .andExpect(view().name("acoes"))
                .andExpect(content().string(containsString("PETR4")));
    }

    @Test
    @DisplayName("Ticker fora do formato volta para a tela com erro no campo")
    void deveValidarFormularioDeAcao() throws Exception {
        mvc.perform(post("/gestao/acoes").with(investidor()).with(csrf())
                        .param("ticker", "pet r4!").param("mercado", "BRASIL"))
                .andExpect(status().isOk())
                .andExpect(view().name("acoes"))
                .andExpect(model().attributeHasFieldErrors("acaoForm", "ticker"))
                .andExpect(content().string(containsString("Ticker inválido")));
    }

    @Test
    @DisplayName("Falha do provedor é mostrada na tela sem cadastrar a ação")
    void deveMostrarFalhaDoProvedorAoCadastrarAcao() throws Exception {
        when(gateway.buscar("XXXX3")).thenThrow(new ServicoExternoException("brapi", "Ticker brasileiro não encontrado: XXXX3"));

        mvc.perform(post("/gestao/acoes").with(investidor()).with(csrf())
                        .param("ticker", "XXXX3").param("mercado", "BRASIL"))
                .andExpect(status().isOk())
                .andExpect(view().name("acoes"))
                .andExpect(content().string(containsString("Ticker brasileiro não encontrado: XXXX3")));
        assertThat(acaoRepository.findAllByUsuarioIdOrderByTickerAsc(conta.getId())).isEmpty();
    }

    @Test
    @DisplayName("Ticker repetido na mesma conta é recusado na tela")
    void deveRecusarTickerRepetido() throws Exception {
        acaoRepository.save(acao("PETR4", "0", "0"));

        mvc.perform(post("/gestao/acoes").with(investidor()).with(csrf())
                        .param("ticker", "PETR4").param("mercado", "BRASIL"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Este ticker já está cadastrado na sua carteira.")));
    }

    @Test
    @DisplayName("POST sem token CSRF é bloqueado no portal")
    void deveExigirCsrfNosFormularios() throws Exception {
        mvc.perform(post("/gestao/acoes").with(investidor()).param("ticker", "PETR4").param("mercado", "BRASIL"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Atualizar cotação troca o preço e acrescenta um ponto ao histórico")
    void deveAtualizarCotacaoPelaTela() throws Exception {
        Acao acao = acaoRepository.save(acao("PETR4", "10", "20"));
        when(gateway.buscar("PETR4")).thenReturn(cotacao("41.00"));

        mvc.perform(post("/gestao/acoes/{id}/atualizar", acao.getId()).with(investidor()).with(csrf()))
                .andExpect(redirectedUrl("/gestao/acoes"))
                .andExpect(flash().attribute("sucesso", "Cotação atualizada e adicionada ao histórico."));

        assertThat(acao.getCotacaoAtual()).isEqualByComparingTo("41.00");
        assertThat(historicoRepository.findTop30ByAcaoIdOrderByDataHoraDesc(acao.getId())).singleElement()
                .satisfies(ponto -> assertThat(ponto.getValor()).isEqualByComparingTo("41.00"));
    }

    @Test
    @DisplayName("Falha do provedor ao atualizar cotação preserva o preço e avisa o usuário")
    void deveAvisarFalhaAoAtualizarCotacao() throws Exception {
        Acao acao = acaoRepository.save(acao("PETR4", "10", "20"));
        when(gateway.buscar(anyString())).thenThrow(new ServicoExternoException("brapi", "Provedor indisponível"));

        mvc.perform(post("/gestao/acoes/{id}/atualizar", acao.getId()).with(investidor()).with(csrf()))
                .andExpect(redirectedUrl("/gestao/acoes"))
                .andExpect(flash().attribute("erro", "Provedor indisponível"));

        assertThat(acao.getCotacaoAtual()).isEqualByComparingTo("25");
    }

    @Test
    @DisplayName("Posição válida altera quantidade e preço médio")
    void deveAtualizarPosicaoPelaTela() throws Exception {
        Acao acao = acaoRepository.save(acao("PETR4", "10", "20"));

        mvc.perform(post("/gestao/acoes/{id}/posicao", acao.getId()).with(investidor()).with(csrf())
                        .param("quantidade", "15").param("precoMedio", "22.50"))
                .andExpect(redirectedUrl("/gestao/acoes"))
                .andExpect(flash().attribute("sucesso", "Posição atualizada. Os gráficos foram recalculados."));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("15");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("22.50");
    }

    @ParameterizedTest
    @CsvSource({"0, 10", "-1, 10", "1.5, 10", "5, 0", "5, -2"})
    @DisplayName("Posição inválida é recusada sem alterar a carteira")
    void deveRecusarPosicaoInvalida(String quantidade, String precoMedio) throws Exception {
        Acao acao = acaoRepository.save(acao("PETR4", "10", "20"));

        mvc.perform(post("/gestao/acoes/{id}/posicao", acao.getId()).with(investidor()).with(csrf())
                        .param("quantidade", quantidade).param("precoMedio", precoMedio))
                .andExpect(redirectedUrl("/gestao/acoes"))
                .andExpect(flash().attribute("erro", startsWith("A quantidade deve ser um número inteiro")));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("10");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("Atualizar cotação de ativo inexistente avisa o usuário sem consultar o provedor")
    void deveAvisarAtivoInexistenteAoAtualizarCotacao() throws Exception {
        mvc.perform(post("/gestao/acoes/{id}/atualizar", 999_999).with(investidor()).with(csrf()))
                .andExpect(redirectedUrl("/gestao/acoes"))
                .andExpect(flash().attribute("erro", "Ação não encontrada na sua carteira."));

        verifyNoInteractions(gateway);
    }

    @Test
    @DisplayName("Atualizar posição de ativo inexistente avisa o usuário")
    void deveAvisarAtivoInexistenteAoAtualizarPosicao() throws Exception {
        mvc.perform(post("/gestao/acoes/{id}/posicao", 999_999).with(investidor()).with(csrf())
                        .param("quantidade", "15").param("precoMedio", "22.50"))
                .andExpect(redirectedUrl("/gestao/acoes"))
                .andExpect(flash().attribute("erro", "Ação não encontrada na sua carteira."));
    }

    @Test
    @DisplayName("Ativo de outra conta recebe o mesmo aviso e continua intacto")
    void naoDeveAlterarAtivoDeOutraContaPeloPortal() throws Exception {
        Usuario outra = new Usuario();
        outra.setNome("Outra conta");
        outra.setEmail("outra-conta-web@carteira.test");
        outra.setSenha("senha-nao-utilizada-no-mockmvc");
        Acao alheia = acao("VALE3", "10", "20");
        alheia.setUsuario(usuarioRepository.save(outra));
        acaoRepository.save(alheia);

        mvc.perform(post("/gestao/acoes/{id}/posicao", alheia.getId()).with(investidor()).with(csrf())
                        .param("quantidade", "99").param("precoMedio", "1"))
                .andExpect(redirectedUrl("/gestao/acoes"))
                .andExpect(flash().attribute("erro", "Ação não encontrada na sua carteira."));
        mvc.perform(post("/gestao/acoes/{id}/atualizar", alheia.getId()).with(investidor()).with(csrf()))
                .andExpect(redirectedUrl("/gestao/acoes"))
                .andExpect(flash().attribute("erro", "Ação não encontrada na sua carteira."));

        verifyNoInteractions(gateway);
        assertThat(alheia.getQuantidade()).isEqualByComparingTo("10");
        assertThat(alheia.getPrecoMedio()).isEqualByComparingTo("20");
    }

    @Test
    @DisplayName("Corretora aprovada nas três consultas é cadastrada")
    void deveCadastrarCorretoraValidada() throws Exception {
        stubCorretoraRegistrada(true);
        when(viaCep.buscarPorCep("22440032")).thenReturn(endereco());

        mvc.perform(post("/gestao/corretoras").with(investidor()).with(csrf())
                        .param("cnpj", "02.332.886/0001-04").param("cep", "22440-032").param("numero", "153"))
                .andExpect(redirectedUrl("/gestao/corretoras"))
                .andExpect(flash().attribute("sucesso", "Corretora validada e cadastrada com sucesso."));

        Corretora salva = corretoraRepository.findByCnpjAndUsuarioId(CNPJ, conta.getId()).orElseThrow();
        assertThat(salva.getRazaoSocial()).isEqualTo("XP CORRETORA");
        assertThat(salva.getCidade()).isEqualTo("Rio de Janeiro");
        mvc.perform(get("/gestao/corretoras").with(investidor()))
                .andExpect(view().name("corretoras"))
                .andExpect(content().string(containsString("XP CORRETORA")));
    }

    @Test
    @DisplayName("Formulário de corretora inválido volta com erro nos campos")
    void deveValidarFormularioDeCorretora() throws Exception {
        mvc.perform(post("/gestao/corretoras").with(investidor()).with(csrf())
                        .param("cnpj", "123").param("cep", "abc").param("numero", "sn"))
                .andExpect(status().isOk())
                .andExpect(view().name("corretoras"))
                .andExpect(model().attributeHasFieldErrors("corretoraForm", "cnpj", "cep", "numero"));
    }

    @Test
    @DisplayName("Instituição sem registro na CVM não é cadastrada")
    void deveRecusarInstituicaoSemRegistroNaCvm() throws Exception {
        stubCorretoraRegistrada(false);

        mvc.perform(post("/gestao/corretoras").with(investidor()).with(csrf())
                        .param("cnpj", CNPJ).param("cep", "22440032").param("numero", "153"))
                .andExpect(status().isOk())
                .andExpect(view().name("corretoras"))
                .andExpect(content().string(containsString("não consta como corretora autorizada na CVM")));
        assertThat(corretoraRepository.findAllByUsuarioIdOrderByRazaoSocialAsc(conta.getId())).isEmpty();
    }

    @Test
    @DisplayName("Falha do serviço de CEP é mostrada na tela de corretoras")
    void deveMostrarFalhaDoServicoDeCep() throws Exception {
        stubCorretoraRegistrada(true);
        when(viaCep.buscarPorCep("22440032")).thenThrow(new ServicoExternoException("ViaCEP", "Não foi possível consultar o CEP agora."));

        mvc.perform(post("/gestao/corretoras").with(investidor()).with(csrf())
                        .param("cnpj", CNPJ).param("cep", "22440032").param("numero", "153"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Não foi possível consultar o CEP agora.")));
    }

    @Test
    @DisplayName("Tela de negociação já abre com o ativo escolhido")
    void devePreSelecionarAtivoNaTelaDeNegociacao() throws Exception {
        Acao acao = acaoRepository.save(acao("PETR4", "10", "20"));

        var resultado = mvc.perform(get("/gestao/negociacoes").param("acaoId", acao.getId().toString()).with(investidor()))
                .andExpect(status().isOk())
                .andExpect(view().name("negociacoes"))
                .andReturn();

        var form = (RegistrarNegociacaoRequest) resultado.getModelAndView().getModel().get("negociacaoForm");
        assertThat(form.getAcaoId()).isEqualTo(acao.getId());
    }

    @Test
    @DisplayName("Compra registrada recalcula a posição e confirma a operação")
    void deveRegistrarCompraPelaTela() throws Exception {
        Acao acao = acaoRepository.save(acao("PETR4", "10", "20"));

        mvc.perform(negociacao(acao.getId(), "COMPRA", "10", "30"))
                .andExpect(redirectedUrl("/gestao/negociacoes"))
                .andExpect(flash().attribute("sucesso", "Compra de 10 PETR4 registrada. A posição foi recalculada."));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("20");
        assertThat(acao.getPrecoMedio()).isEqualByComparingTo("25");
        assertThat(negociacaoRepository.findTop200ByAcaoUsuarioIdOrderByDataNegociacaoDescIdDesc(conta.getId())).hasSize(1);
    }

    @Test
    @DisplayName("Formulário de negociação incompleto volta com erro nos campos")
    void deveValidarFormularioDeNegociacao() throws Exception {
        mvc.perform(post("/gestao/negociacoes").with(investidor()).with(csrf()).param("quantidade", "0"))
                .andExpect(status().isOk())
                .andExpect(view().name("negociacoes"))
                .andExpect(model().attributeHasFieldErrors("negociacaoForm", "acaoId", "tipo", "quantidade", "precoUnitario"));
    }

    @Test
    @DisplayName("Venda acima do saldo é recusada na tela sem registrar a operação")
    void deveRecusarVendaAcimaDoSaldoPelaTela() throws Exception {
        Acao acao = acaoRepository.save(acao("PETR4", "10", "20"));

        mvc.perform(negociacao(acao.getId(), "VENDA", "11", "30"))
                .andExpect(status().isOk())
                .andExpect(view().name("negociacoes"))
                .andExpect(content().string(containsString("supera o saldo disponível de 10 unidades")));

        assertThat(acao.getQuantidade()).isEqualByComparingTo("10");
    }

    @Test
    @DisplayName("Negociação sobre ativo inexistente é recusada na tela")
    void deveRecusarNegociacaoDeAtivoInexistente() throws Exception {
        mvc.perform(negociacao(999_999L, "COMPRA", "1", "30"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Ação não encontrada na sua carteira.")));
    }

    @Test
    @DisplayName("Imposto de Renda usa o ano pedido quando há operações nele")
    void deveFiltrarImpostoDeRendaPeloAnoPedido() throws Exception {
        Acao acao = acaoRepository.save(acao("PETR4", "10", "20"));
        mvc.perform(post("/gestao/negociacoes").with(investidor()).with(csrf())
                        .param("acaoId", acao.getId().toString()).param("tipo", "COMPRA")
                        .param("quantidade", "1").param("precoUnitario", "30")
                        .param("dataNegociacao", "2025-06-15T10:00:00"))
                .andExpect(redirectedUrl("/gestao/negociacoes"));

        mvc.perform(get("/imposto-renda").param("ano", "2025").with(investidor()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("anoSelecionado", 2025));
        mvc.perform(get("/imposto-renda").param("ano", "1999").with(investidor()))
                .andExpect(status().isOk())
                .andExpect(model().attribute("anoSelecionado", 2025));
    }

    @Test
    @DisplayName("Sem operações, Imposto de Renda mantém o ano pedido ou usa o ano atual")
    void deveEscolherAnoSemOperacoes() throws Exception {
        mvc.perform(get("/imposto-renda").param("ano", "2020").with(investidor()))
                .andExpect(model().attribute("anoSelecionado", 2020));
        mvc.perform(get("/imposto-renda").with(investidor()))
                .andExpect(model().attribute("anoSelecionado", LocalDateTime.now().getYear()));
    }

    private RequestBuilder negociacao(Long acaoId, String tipo, String quantidade, String preco) {
        return post("/gestao/negociacoes").with(investidor()).with(csrf())
                .param("acaoId", String.valueOf(acaoId)).param("tipo", tipo)
                .param("quantidade", quantidade).param("precoUnitario", preco)
                .param("dataNegociacao", LocalDateTime.now().minusHours(1).withNano(0).toString());
    }

    private void stubCorretoraRegistrada(boolean registrada) {
        when(brasilApi.buscarPorCnpj(CNPJ)).thenReturn(new EmpresaDados(CNPJ, "XP CORRETORA", "XP",
                "contato@xp.test", "2130000000", "22440032", "ATIVA"));
        when(brasilApi.validarCorretora(CNPJ)).thenReturn(new CvmDados(registrada, "XP CORRETORA", "EM FUNCIONAMENTO"));
    }

    private static EnderecoDados endereco() {
        return new EnderecoDados("22440032", "Avenida Ataulfo de Paiva", "lado impar", "Leblon", "Rio de Janeiro", "RJ");
    }

    private static CotacaoDados cotacao(String preco) {
        return new CotacaoDados("PETR4", "Petrobras", "BRL", new BigDecimal(preco), BigDecimal.ONE,
                LocalDateTime.now().minusMinutes(5), List.of());
    }

    private Acao acao(String ticker, String quantidade, String precoMedio) {
        Acao acao = new Acao();
        acao.setUsuario(conta);
        acao.setTicker(ticker);
        acao.setNomeEmpresa("Petrobras");
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda("BRL");
        acao.setCotacaoAtual(new BigDecimal("25"));
        acao.setVariacaoPercentual(BigDecimal.ZERO);
        acao.setQuantidade(new BigDecimal(quantidade));
        acao.setPrecoMedio(new BigDecimal(precoMedio));
        acao.setDataHoraCotacao(LocalDateTime.now());
        return acao;
    }

    private static UserRequestPostProcessor investidor() {
        return user(EMAIL).roles("USER");
    }
}
