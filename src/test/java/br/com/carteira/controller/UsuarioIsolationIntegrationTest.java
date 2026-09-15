package br.com.carteira.controller;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.carteira.model.Acao;
import br.com.carteira.model.Mercado;
import br.com.carteira.model.Negociacao;
import br.com.carteira.model.TipoNegociacao;
import br.com.carteira.model.Usuario;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.NegociacaoRepository;
import br.com.carteira.repository.UsuarioRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioIsolationIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired AcaoRepository acaoRepository;
    @Autowired NegociacaoRepository negociacaoRepository;

    @Test
    void novaContaDeveComecarVaziaENaoEnxergarAtivosDeOutraConta() throws Exception {
        Usuario primeiraConta = usuario("primeira-isolamento@carteira.test");
        Usuario novaConta = usuario("nova-isolamento@carteira.test");
        Acao acaoAlheia = acaoRepository.save(acao(primeiraConta, "ONLYA"));
        negociacaoRepository.save(negociacao(acaoAlheia));

        mvc.perform(get("/gestao/acoes").with(user(novaConta.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Nenhum ativo cadastrado")))
                .andExpect(content().string(not(containsString("ONLYA"))));

        mvc.perform(get("/dashboard/dados").with(user(novaConta.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAtivos").value(0))
                .andExpect(jsonPath("$.totalCorretoras").value(0));

        mvc.perform(get("/acoes/{id}", acaoAlheia.getId()).with(user(novaConta.getEmail()).roles("USER")))
                .andExpect(status().isNotFound());

        mvc.perform(get("/gestao/negociacoes").with(user(novaConta.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("ONLYA"))));

        mvc.perform(get("/historico").with(user(novaConta.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("ONLYA"))));

        mvc.perform(get("/imposto-renda").with(user(novaConta.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("ONLYA"))));
    }

    @Test
    void cadaContaDeveVisualizarSomenteSuaPropriaCarteira() throws Exception {
        Usuario primeiraConta = usuario("dono-a@carteira.test");
        Usuario segundaConta = usuario("dono-b@carteira.test");
        acaoRepository.save(acao(primeiraConta, "OWNAA"));
        acaoRepository.save(acao(segundaConta, "OWNBB"));

        mvc.perform(get("/gestao/acoes").with(user(primeiraConta.getEmail()).roles("USER")))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("OWNAA")))
                .andExpect(content().string(not(containsString("OWNBB"))));
    }

    @Test
    void mesmoTickerDeveSerIndependenteEmContasDiferentes() {
        Usuario primeiraConta = usuario("ticker-a@carteira.test");
        Usuario segundaConta = usuario("ticker-b@carteira.test");

        Acao primeira = acaoRepository.saveAndFlush(acao(primeiraConta, "PETR4"));
        Acao segunda = acaoRepository.saveAndFlush(acao(segundaConta, "PETR4"));

        assertNotEquals(primeira.getId(), segunda.getId());
        assertEquals(1, acaoRepository.findAllByUsuarioIdOrderByTickerAsc(primeiraConta.getId()).size());
        assertEquals(1, acaoRepository.findAllByUsuarioIdOrderByTickerAsc(segundaConta.getId()).size());
    }

    private Usuario usuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Investidor isolado");
        usuario.setEmail(email);
        usuario.setSenha("senha-nao-utilizada-no-mockmvc");
        return usuarioRepository.save(usuario);
    }

    private Acao acao(Usuario usuario, String ticker) {
        Acao acao = new Acao();
        acao.setUsuario(usuario);
        acao.setTicker(ticker);
        acao.setNomeEmpresa(ticker + " Participações");
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda("BRL");
        acao.setCotacaoAtual(new BigDecimal("20.00"));
        acao.setVariacaoPercentual(BigDecimal.ZERO);
        acao.setQuantidade(new BigDecimal("10"));
        acao.setPrecoMedio(new BigDecimal("18.00"));
        acao.setDataHoraCotacao(LocalDateTime.now());
        return acao;
    }

    private Negociacao negociacao(Acao acao) {
        Negociacao negociacao = new Negociacao();
        negociacao.setAcao(acao);
        negociacao.setTipo(TipoNegociacao.COMPRA);
        negociacao.setQuantidade(new BigDecimal("10"));
        negociacao.setPrecoUnitario(new BigDecimal("18.00"));
        negociacao.setTaxas(BigDecimal.ZERO);
        negociacao.setValorBruto(new BigDecimal("180.00"));
        negociacao.setValorLiquido(new BigDecimal("180.00"));
        negociacao.setResultadoRealizado(BigDecimal.ZERO);
        negociacao.setDataNegociacao(LocalDateTime.now().minusDays(1));
        return negociacao;
    }
}
