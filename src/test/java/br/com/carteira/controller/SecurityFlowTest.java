package br.com.carteira.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.carteira.model.Usuario;
import br.com.carteira.repository.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityFlowTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarioRepository;

    @BeforeEach
    void prepararUsuarioDasTelasProtegidas() {
        if (!usuarioRepository.existsByEmailIgnoreCase("teste@carteira.com")) {
            Usuario usuario = new Usuario();
            usuario.setNome("Usuário de teste");
            usuario.setEmail("teste@carteira.com");
            usuario.setSenha("senha-nao-utilizada-no-mockmvc");
            usuarioRepository.save(usuario);
        }
    }

    @Test
    void deveRedirecionarUsuarioNaoAutenticadoParaLogin() throws Exception {
        mvc.perform(get("/dashboard").accept(MediaType.TEXT_HTML))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void deveRenderizarDashboardParaUsuarioAutenticado() throws Exception {
        mvc.perform(get("/dashboard").with(user("teste@carteira.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard"))
                .andExpect(content().string(containsString("averagePriceChart")))
                .andExpect(content().string(containsString("averageComparisonChart")))
                .andExpect(content().string(containsString("averageDistanceChart")))
                .andExpect(content().string(containsString("averageHistoryChart")))
                .andExpect(content().string(containsString("averageLogoStrip")))
                .andExpect(content().string(containsString("class=\"app-nav\"")))
                .andExpect(content().string(containsString("data-theme-toggle")))
                .andExpect(content().string(containsString("Preço médio")))
                .andExpect(content().string(containsString("Resultados")))
                .andExpect(content().string(containsString("Posições")))
                .andExpect(content().string(containsString("Histórico")))
                .andExpect(content().string(containsString("Imposto de Renda")))
                .andExpect(content().string(containsString("Configurações")))
                .andExpect(content().string(containsString("brand-chart")))
                .andExpect(content().string(containsString("Todos os ativos cadastrados")));
    }

    @Test
    void deveRenderizarNovasTelasDoPortalParaUsuarioAutenticado() throws Exception {
        var usuario = user("teste@carteira.com").roles("USER");
        mvc.perform(get("/configuracoes").with(usuario))
                .andExpect(status().isOk())
                .andExpect(view().name("configuracoes"))
                .andExpect(content().string(containsString("Preto piano")))
                .andExpect(content().string(containsString("data-density-choice")));
        mvc.perform(get("/historico").with(usuario))
                .andExpect(status().isOk())
                .andExpect(view().name("historico"))
                .andExpect(content().string(containsString("Movimentações completas")));
        mvc.perform(get("/imposto-renda").with(usuario))
                .andExpect(status().isOk())
                .andExpect(view().name("imposto-renda"))
                .andExpect(content().string(containsString("não declaração oficial")))
                .andExpect(content().string(containsString("BRL e USD nunca são somados")));
    }

    @Test
    void deveServirTexturaOriginalDoProjetoSemAutenticacao() throws Exception {
        mvc.perform(get("/images/marble-black-gold.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_PNG));
    }

    @Test
    void deveDisponibilizarAlternanciaDeTemaNaTelaDeLogin() throws Exception {
        mvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("/js/theme.js")))
                .andExpect(content().string(containsString("data-theme-toggle")))
                .andExpect(content().string(containsString("Modo claro")));
    }

    @Test
    void deveRenderizarNegociacaoComPrecoEditavelESemTaxaManual() throws Exception {
        mvc.perform(get("/gestao/negociacoes").with(user("teste@carteira.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("negociacoes"))
                .andExpect(content().string(containsString("Preço da compra ou venda")))
                .andExpect(content().string(containsString("data-numeric=\"decimal\"")))
                .andExpect(content().string(containsString("name=\"precoUnitario\"")))
                .andExpect(content().string(containsString("data-numeric=\"integer\"")))
                .andExpect(content().string(containsString("step=\"1\"")))
                .andExpect(content().string(not(containsString("name=\"taxas\""))));
    }

    @Test
    void deveOcultarDocumentacaoOpenApiDeUsuarioComum() throws Exception {
        mvc.perform(get("/v3/api-docs").with(user("usuario@carteira.com").roles("USER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void deveExporDocumentacaoOpenApiSomenteParaAdministrador() throws Exception {
        mvc.perform(get("/v3/api-docs").with(user("admin@carteira.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.openapi").exists())
                .andExpect(jsonPath("$.paths['/api/negociacoes']").exists());
    }
}
