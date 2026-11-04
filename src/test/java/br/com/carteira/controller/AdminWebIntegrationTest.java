package br.com.carteira.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.response.SecurityMockMvcResultMatchers.unauthenticated;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.com.carteira.dto.PainelAdmin;
import br.com.carteira.model.AcaoAdmin;
import br.com.carteira.model.Acao;
import br.com.carteira.model.AtividadeAdmin;
import br.com.carteira.model.Mercado;
import br.com.carteira.model.Papel;
import br.com.carteira.model.Usuario;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.AtividadeAdminRepository;
import br.com.carteira.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Área de administração: só ADMIN entra, as carteiras continuam privadas e toda mudança fica registrada. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminWebIntegrationTest {
    private static final String ADMIN = "admin-portal@carteira.test";
    private static final String INVESTIDOR = "investidor-portal@carteira.test";
    private static final String SENHA = "SenhaForte123";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired AcaoRepository acaoRepository;
    @Autowired AtividadeAdminRepository atividadeRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private Usuario admin;
    private Usuario investidor;

    @BeforeEach
    void criarContas() {
        admin = conta(ADMIN, Papel.ADMIN);
        investidor = conta(INVESTIDOR, Papel.USER);
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/admin/usuarios"})
    @DisplayName("Visitante é levado ao login")
    void visitanteDeveIrParaOLogin(String rota) throws Exception {
        mvc.perform(get(rota))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"/admin", "/admin/usuarios"})
    @DisplayName("Investidor recebe 403 nas telas de administração")
    void investidorNaoDeveAbrirAdministracao(String rota) throws Exception {
        mvc.perform(get(rota).with(comoInvestidor())).andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Investidor não consegue se promover nem alterar outras contas")
    void investidorNaoDeveExecutarAcoesAdministrativas() throws Exception {
        mvc.perform(post("/admin/usuarios/{id}/papel", investidor.getId()).with(comoInvestidor()).with(csrf())
                        .param("papel", "ADMIN"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/usuarios/{id}/situacao", admin.getId()).with(comoInvestidor()).with(csrf())
                        .param("ativo", "false"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/usuarios/{id}/senha", admin.getId()).with(comoInvestidor()).with(csrf())
                        .param("senha", "Invadida123"))
                .andExpect(status().isForbidden());
        mvc.perform(post("/admin/usuarios").with(comoInvestidor()).with(csrf())
                        .param("nome", "Admin Falso").param("email", "falso@carteira.test")
                        .param("senha", SENHA).param("papel", "ADMIN"))
                .andExpect(status().isForbidden());

        assertThat(investidor.getPapel()).isEqualTo(Papel.USER);
        assertThat(admin.isAtivo()).isTrue();
        assertThat(passwordEncoder.matches(SENHA, admin.getSenha())).isTrue();
        assertThat(usuarioRepository.existsByEmailIgnoreCase("falso@carteira.test")).isFalse();
    }

    @Test
    @DisplayName("Formulários da administração exigem CSRF")
    void deveExigirCsrf() throws Exception {
        mvc.perform(post("/admin/usuarios/{id}/papel", investidor.getId()).with(comoAdmin()).param("papel", "ADMIN"))
                .andExpect(status().isForbidden());
        assertThat(investidor.getPapel()).isEqualTo(Papel.USER);
    }

    @Test
    @DisplayName("Somente o administrador vê o link Administração no menu")
    void menuDeveMostrarAdministracaoSomenteParaAdmin() throws Exception {
        mvc.perform(get("/dashboard").with(comoAdmin()))
                .andExpect(content().string(containsString("href=\"/admin\"")))
                .andExpect(content().string(containsString("Administrador")));
        mvc.perform(get("/dashboard").with(comoInvestidor()))
                .andExpect(content().string(not(containsString("href=\"/admin\""))))
                .andExpect(content().string(not(containsString(">Administração<"))));
    }

    @Test
    @DisplayName("Administrador entra pelo formulário real e abre o painel")
    void administradorDeveEntrarPeloFormularioEAbrirOPainel() throws Exception {
        MockHttpSession sessao = (MockHttpSession) mvc.perform(formLogin("/login").userParameter("email")
                        .user(ADMIN).password(SENHA))
                .andExpect(redirectedUrl("/dashboard"))
                .andReturn().getRequest().getSession(false);

        mvc.perform(get("/admin").session(sessao))
                .andExpect(status().isOk())
                .andExpect(view().name("admin"))
                .andExpect(content().string(containsString("Painel do sistema")));
    }

    @Test
    @DisplayName("Token da API não abre a área de administração do portal")
    void tokenDaApiNaoDeveAbrirOPortal() throws Exception {
        String corpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(ADMIN, SENHA)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(corpo, "$.token");

        mvc.perform(get("/admin").header("Authorization", "Bearer " + token))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Painel soma as contas novas, desativadas e administradores")
    void painelDeveRefletirAsContas() throws Exception {
        PainelAdmin antes = painel();
        Usuario desativada = conta("desativada-portal@carteira.test", Papel.USER);
        desativada.setAtivo(false);
        conta("segundo-admin-portal@carteira.test", Papel.ADMIN);

        PainelAdmin depois = painel();

        assertThat(depois.totalContas()).isEqualTo(antes.totalContas() + 2);
        assertThat(depois.contasNovas30Dias()).isEqualTo(antes.contasNovas30Dias() + 2);
        assertThat(depois.contasDesativadas()).isEqualTo(antes.contasDesativadas() + 1);
        assertThat(depois.administradores()).isEqualTo(antes.administradores() + 1);
        assertThat(depois.contasAtivas() + depois.contasDesativadas()).isEqualTo(depois.totalContas());
    }

    @Test
    @DisplayName("Painel conta os ativos de todas as carteiras sem mostrar nenhum deles")
    void painelNaoDeveExporCarteiras() throws Exception {
        long ativosAntes = painel().totalAtivos();
        acaoRepository.save(acaoSigilosa());

        assertThat(painel().totalAtivos()).isEqualTo(ativosAntes + 1);
        mvc.perform(get("/admin").with(comoAdmin()))
                .andExpect(content().string(not(containsString("SECR3"))))
                .andExpect(content().string(not(containsString("Empresa Sigilosa"))))
                .andExpect(content().string(not(containsString("987,65"))));
        mvc.perform(get("/admin/usuarios").with(comoAdmin()))
                .andExpect(content().string(not(containsString("SECR3"))))
                .andExpect(content().string(not(containsString(admin.getSenha()))))
                .andExpect(content().string(not(containsString("$2a$"))));
    }

    @Test
    @DisplayName("Tela de contas lista as contas e protege a própria conta do administrador")
    void telaDeContasDeveListarEProtegerAPropriaConta() throws Exception {
        mvc.perform(get("/admin/usuarios").with(comoAdmin()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-usuarios"))
                .andExpect(content().string(containsString(INVESTIDOR)))
                .andExpect(content().string(containsString("Sua conta")))
                .andExpect(content().string(containsString("Tornar administrador")));
    }

    @Test
    @DisplayName("Administrador cria conta com hash BCrypt e a criação fica registrada sem a senha")
    void deveCriarContaERegistrarAtividade() throws Exception {
        mvc.perform(post("/admin/usuarios").with(comoAdmin()).with(csrf())
                        .param("nome", "Nova Investidora").param("email", "Nova.Conta@Carteira.Test")
                        .param("senha", "SenhaDaNova123").param("papel", "USER"))
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("sucesso", "Conta de nova.conta@carteira.test criada."));

        Usuario criada = usuarioRepository.findByEmailIgnoreCase("nova.conta@carteira.test").orElseThrow();
        assertThat(criada.getPapel()).isEqualTo(Papel.USER);
        assertThat(passwordEncoder.matches("SenhaDaNova123", criada.getSenha())).isTrue();
        AtividadeAdmin atividade = ultimaAtividade();
        assertThat(atividade.getResponsavel()).isEqualTo(ADMIN);
        assertThat(atividade.getAcao()).isEqualTo(AcaoAdmin.CRIOU_CONTA);
        assertThat(atividade.getConta()).isEqualTo("nova.conta@carteira.test");
        assertThat(atividade.getDetalhe()).isEqualTo("Papel: Investidor").doesNotContain("SenhaDaNova123");
    }

    @Test
    @DisplayName("Formulário de conta inválido volta com erros e sem repetir a senha")
    void deveValidarFormularioDeConta() throws Exception {
        mvc.perform(post("/admin/usuarios").with(comoAdmin()).with(csrf())
                        .param("nome", "Ab").param("email", "sem-arroba").param("senha", "fraca123")
                        .param("papel", "USER"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-usuarios"))
                .andExpect(model().attributeHasFieldErrors("usuarioForm", "nome", "email", "senha"))
                .andExpect(content().string(not(containsString("value=\"fraca123\""))));
    }

    @Test
    @DisplayName("E-mail já usado é recusado na tela de contas")
    void deveRecusarEmailDuplicado() throws Exception {
        mvc.perform(post("/admin/usuarios").with(comoAdmin()).with(csrf())
                        .param("nome", "Outra Pessoa").param("email", INVESTIDOR.toUpperCase())
                        .param("senha", SENHA).param("papel", "USER"))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("Já existe uma conta com este e-mail.")));
    }

    @Test
    @DisplayName("Promover e rebaixar uma conta altera o papel e registra cada mudança")
    void devePromoverERebaixarConta() throws Exception {
        mvc.perform(post("/admin/usuarios/{id}/papel", investidor.getId()).with(comoAdmin()).with(csrf())
                        .param("papel", "ADMIN"))
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("sucesso", INVESTIDOR + " agora é administrador."));
        assertThat(investidor.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(ultimaAtividade().getDetalhe()).isEqualTo("Investidor para Administrador");

        mvc.perform(post("/admin/usuarios/{id}/papel", investidor.getId()).with(comoAdmin()).with(csrf())
                        .param("papel", "USER"))
                .andExpect(flash().attribute("sucesso", INVESTIDOR + " agora é investidor."));
        assertThat(investidor.getPapel()).isEqualTo(Papel.USER);
    }

    @Test
    @DisplayName("Conta desativada não entra mais pelo formulário até ser reativada")
    void deveDesativarEReativarConta() throws Exception {
        mvc.perform(post("/admin/usuarios/{id}/situacao", investidor.getId()).with(comoAdmin()).with(csrf())
                        .param("ativo", "false"))
                .andExpect(flash().attribute("sucesso", "Conta de " + INVESTIDOR + " desativada."));
        assertThat(ultimaAtividade().getAcao()).isEqualTo(AcaoAdmin.DESATIVOU_CONTA);
        mvc.perform(formLogin("/login").userParameter("email").user(INVESTIDOR).password(SENHA))
                .andExpect(unauthenticated());

        mvc.perform(post("/admin/usuarios/{id}/situacao", investidor.getId()).with(comoAdmin()).with(csrf())
                        .param("ativo", "true"))
                .andExpect(flash().attribute("sucesso", "Conta de " + INVESTIDOR + " reativada."));
        assertThat(ultimaAtividade().getAcao()).isEqualTo(AcaoAdmin.REATIVOU_CONTA);
        mvc.perform(formLogin("/login").userParameter("email").user(INVESTIDOR).password(SENHA))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Senha redefinida passa a valer e a antiga deixa de autenticar")
    void deveRedefinirSenha() throws Exception {
        mvc.perform(post("/admin/usuarios/{id}/senha", investidor.getId()).with(comoAdmin()).with(csrf())
                        .param("senha", "NovaSenha456"))
                .andExpect(flash().attribute("sucesso", "Senha de " + INVESTIDOR + " redefinida."));

        assertThat(passwordEncoder.matches("NovaSenha456", investidor.getSenha())).isTrue();
        assertThat(passwordEncoder.matches(SENHA, investidor.getSenha())).isFalse();
        assertThat(ultimaAtividade().getAcao()).isEqualTo(AcaoAdmin.REDEFINIU_SENHA);
        assertThat(ultimaAtividade().getDetalhe()).isNull();
    }

    @Test
    @DisplayName("Senha fora da política não é gravada")
    void deveRecusarSenhaFraca() throws Exception {
        mvc.perform(post("/admin/usuarios/{id}/senha", investidor.getId()).with(comoAdmin()).with(csrf())
                        .param("senha", "1234567"))
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("erro", "Use ao menos 8 caracteres, com maiúscula, minúscula e número"));

        assertThat(passwordEncoder.matches(SENHA, investidor.getSenha())).isTrue();
    }

    @Test
    @DisplayName("Administrador não altera o papel nem desativa a própria conta")
    void naoDeveAlterarAPropriaConta() throws Exception {
        mvc.perform(post("/admin/usuarios/{id}/papel", admin.getId()).with(comoAdmin()).with(csrf())
                        .param("papel", "USER"))
                .andExpect(flash().attribute("erro", "Você não pode alterar o papel da sua própria conta."));
        mvc.perform(post("/admin/usuarios/{id}/situacao", admin.getId()).with(comoAdmin()).with(csrf())
                        .param("ativo", "false"))
                .andExpect(flash().attribute("erro", "Você não pode desativar a sua própria conta."));

        assertThat(admin.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(admin.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("O último administrador ativo não pode ser desativado nem rebaixado")
    void deveManterUmAdministradorAtivo() throws Exception {
        UserRequestPostProcessor outroAdmin = user("admin-externo@carteira.test").roles("ADMIN");

        mvc.perform(post("/admin/usuarios/{id}/situacao", admin.getId()).with(outroAdmin).with(csrf())
                        .param("ativo", "false"))
                .andExpect(flash().attribute("erro", "O sistema precisa manter pelo menos um administrador ativo."));
        mvc.perform(post("/admin/usuarios/{id}/papel", admin.getId()).with(outroAdmin).with(csrf())
                        .param("papel", "USER"))
                .andExpect(flash().attribute("erro", "O sistema precisa manter pelo menos um administrador ativo."));

        assertThat(admin.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(admin.isAtivo()).isTrue();
    }

    @Test
    @DisplayName("Ação sobre conta inexistente mostra aviso em vez de erro")
    void deveAvisarContaInexistente() throws Exception {
        mvc.perform(post("/admin/usuarios/{id}/situacao", 999_999).with(comoAdmin()).with(csrf()).param("ativo", "false"))
                .andExpect(redirectedUrl("/admin/usuarios"))
                .andExpect(flash().attribute("erro", "Usuário não encontrado."));
        mvc.perform(post("/admin/usuarios/{id}/senha", 999_999).with(comoAdmin()).with(csrf()).param("senha", "NovaSenha456"))
                .andExpect(flash().attribute("erro", "Usuário não encontrado."));
    }

    @Test
    @DisplayName("Painel mostra as atividades registradas")
    void painelDeveListarAtividades() throws Exception {
        mvc.perform(post("/admin/usuarios/{id}/senha", investidor.getId()).with(comoAdmin()).with(csrf())
                .param("senha", "NovaSenha456"));

        mvc.perform(get("/admin").with(comoAdmin()))
                .andExpect(content().string(containsString("Redefiniu a senha")))
                .andExpect(content().string(containsString(INVESTIDOR)));
    }

    private PainelAdmin painel() throws Exception {
        return (PainelAdmin) mvc.perform(get("/admin").with(comoAdmin())).andReturn()
                .getModelAndView().getModel().get("painel");
    }

    private AtividadeAdmin ultimaAtividade() {
        return atividadeRepository.findTop20ByOrderByDataHoraDescIdDesc().get(0);
    }

    private Usuario conta(String email, Papel papel) {
        Usuario usuario = new Usuario();
        usuario.setNome("Conta " + papel.getDescricao());
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setPapel(papel);
        return usuarioRepository.save(usuario);
    }

    private Acao acaoSigilosa() {
        Acao acao = new Acao();
        acao.setUsuario(investidor);
        acao.setTicker("SECR3");
        acao.setNomeEmpresa("Empresa Sigilosa");
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda("BRL");
        acao.setCotacaoAtual(new BigDecimal("987.65"));
        acao.setVariacaoPercentual(BigDecimal.ZERO);
        acao.setQuantidade(new BigDecimal("10"));
        acao.setPrecoMedio(new BigDecimal("987.65"));
        acao.setDataHoraCotacao(LocalDateTime.now());
        return acao;
    }

    private static UserRequestPostProcessor comoAdmin() {
        return user(ADMIN).roles("ADMIN");
    }

    private static UserRequestPostProcessor comoInvestidor() {
        return user(INVESTIDOR).roles("USER");
    }
}
