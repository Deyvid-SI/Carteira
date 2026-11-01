package br.com.carteira.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import br.com.carteira.model.Papel;
import br.com.carteira.model.Usuario;
import br.com.carteira.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CadastroWebIntegrationTest {
    private static final String EMAIL = "novo-cadastro@carteira.test";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("Página inicial leva a conta autenticada ao dashboard")
    void paginaInicialDeveLevarAoDashboard() throws Exception {
        mvc.perform(get("/").with(user("qualquer@carteira.test").roles("USER")))
                .andExpect(redirectedUrl("/dashboard"));
    }

    @Test
    @DisplayName("Página inicial leva o visitante ao login")
    void paginaInicialDeveLevarVisitanteAoLogin() throws Exception {
        mvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @DisplayName("Tela de cadastro é pública")
    void telaDeCadastroDeveSerPublica() throws Exception {
        mvc.perform(get("/cadastro"))
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(model().attributeExists("usuario"));
    }

    @Test
    @DisplayName("Cadastro válido cria conta USER com e-mail normalizado e senha em BCrypt")
    void deveCriarContaUserComSenhaCodificada() throws Exception {
        cadastrar("Nova Investidora", "Novo-Cadastro@Carteira.Test", "SenhaForte123", "SenhaForte123")
                .andExpect(redirectedUrl("/login?cadastrado"));

        Usuario criada = usuarioRepository.findByEmailIgnoreCase(EMAIL).orElseThrow();
        assertThat(criada.getEmail()).isEqualTo(EMAIL);
        assertThat(criada.getPapel()).isEqualTo(Papel.USER);
        assertThat(criada.isAtivo()).isTrue();
        assertThat(criada.getSenha()).startsWith("$2").isNotEqualTo("SenhaForte123");
        assertThat(passwordEncoder.matches("SenhaForte123", criada.getSenha())).isTrue();
    }

    @Test
    @DisplayName("Confirmação diferente da senha volta para a tela sem criar conta")
    void deveRecusarConfirmacaoDiferente() throws Exception {
        cadastrar("Nova Investidora", EMAIL, "SenhaForte123", "SenhaForte124")
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(model().attributeHasFieldErrors("usuario", "confirmarSenha"))
                .andExpect(content().string(containsString("As senhas não conferem")));

        assertThat(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).isFalse();
    }

    @Test
    @DisplayName("E-mail já cadastrado é informado na tela")
    void deveRecusarEmailDuplicado() throws Exception {
        Usuario existente = new Usuario();
        existente.setNome("Conta existente");
        existente.setEmail(EMAIL);
        existente.setSenha("hash-existente");
        usuarioRepository.save(existente);

        cadastrar("Nova Investidora", EMAIL.toUpperCase(), "SenhaForte123", "SenhaForte123")
                .andExpect(status().isOk())
                .andExpect(view().name("cadastro"))
                .andExpect(content().string(containsString("Já existe uma conta com este e-mail.")));
    }

    @ParameterizedTest
    @ValueSource(strings = {"Curta1", "semmaiuscula123", "SEMMINUSCULA123", "SemNumeroAqui"})
    @DisplayName("Senha fora da política é recusada")
    void deveAplicarPoliticaDeSenha(String senha) throws Exception {
        cadastrar("Nova Investidora", EMAIL, senha, senha)
                .andExpect(status().isOk())
                .andExpect(model().attributeHasFieldErrors("usuario", "senha"));

        assertThat(usuarioRepository.existsByEmailIgnoreCase(EMAIL)).isFalse();
    }

    @Test
    @DisplayName("Política de senha aceita exatamente 8 e 72 caracteres e recusa 7 e 73")
    void deveRespeitarLimitesDeTamanhoDaSenha() throws Exception {
        for (int tamanho : new int[] {7, 73}) {
            String senha = "Aa1" + "x".repeat(tamanho - 3);
            cadastrar("Nova Investidora", "limite" + tamanho + "@carteira.test", senha, senha)
                    .andExpect(model().attributeHasFieldErrors("usuario", "senha"));
        }
        for (int tamanho : new int[] {8, 72}) {
            String senha = "Aa1" + "x".repeat(tamanho - 3);
            cadastrar("Nova Investidora", "limite" + tamanho + "@carteira.test", senha, senha)
                    .andExpect(redirectedUrl("/login?cadastrado"));
        }
    }

    private ResultActions cadastrar(String nome, String email, String senha, String confirmacao) throws Exception {
        return mvc.perform(post("/cadastro").with(csrf()).param("nome", nome).param("email", email)
                .param("senha", senha).param("confirmarSenha", confirmacao));
    }
}
