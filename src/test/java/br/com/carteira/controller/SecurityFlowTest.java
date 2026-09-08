package br.com.carteira.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
                .andExpect(content().string(containsString("averagePriceChart")));
    }

    @Test
    void deveRenderizarNegociacaoSemPrecoEditavel() throws Exception {
        mvc.perform(get("/gestao/negociacoes").with(user("teste@carteira.com").roles("USER")))
                .andExpect(status().isOk())
                .andExpect(view().name("negociacoes"))
                .andExpect(content().string(containsString("Preço de execução")))
                .andExpect(content().string(containsString("data-numeric=\"decimal\"")))
                .andExpect(content().string(not(containsString("name=\"precoUnitario\""))))
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
                .andExpect(jsonPath("$.paths['/negociacoes']").exists());
    }
}
