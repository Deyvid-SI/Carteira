package br.com.carteira.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.carteira.model.Papel;
import br.com.carteira.model.Usuario;
import br.com.carteira.repository.UsuarioRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** Regras da Aula 14: somente ADMIN gerencia contas e nenhuma resposta expõe a senha ou o hash. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class UsuarioApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("ADMIN cadastra conta com hash BCrypt e resposta sem senha")
    void adminDeveCadastrarUsuarioSemExporSenha() throws Exception {
        mvc.perform(post("/api/usuarios").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Ana Investidora","email":"Ana.Nova@Carteira.Test",
                                 "senha":"SenhaDaAna123","papel":"USER"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.email").value("ana.nova@carteira.test"))
                .andExpect(jsonPath("$.papel").value("USER"))
                .andExpect(jsonPath("$.ativo").value(true))
                .andExpect(jsonPath("$.senha").doesNotExist())
                .andExpect(jsonPath("$.password").doesNotExist());

        String hash = usuarioRepository.findByEmailIgnoreCase("ana.nova@carteira.test").orElseThrow().getSenha();
        assertThat(hash).startsWith("$2").isNotEqualTo("SenhaDaAna123");
        assertThat(passwordEncoder.matches("SenhaDaAna123", hash)).isTrue();
    }

    @Test
    @DisplayName("E-mail já usado por outra conta é recusado com 422")
    void deveRecusarEmailDuplicado() throws Exception {
        usuario("duplicado-admin@carteira.test");

        mvc.perform(post("/api/usuarios").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Outra pessoa","email":"DUPLICADO-ADMIN@carteira.test",
                                 "senha":"SenhaForte123","papel":"USER"}
                                """))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.detail").value("Já existe uma conta com este e-mail."));
    }

    @Test
    @DisplayName("Senha fora da política é recusada com 400")
    void deveValidarPoliticaDeSenha() throws Exception {
        mvc.perform(post("/api/usuarios").with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Senha fraca","email":"fraca@carteira.test","senha":"1234567","papel":"USER"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.senha").exists());
    }

    @Test
    @DisplayName("ADMIN lista e consulta contas sem receber hashes")
    void adminDeveListarEConsultarUsuarios() throws Exception {
        Usuario usuario = usuario("listado@carteira.test");

        mvc.perform(get("/api/usuarios").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content[*].senha").isEmpty());
        mvc.perform(get("/api/usuarios/{id}", usuario.getId()).with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("listado@carteira.test"))
                .andExpect(jsonPath("$.senha").doesNotExist());
    }

    @Test
    @DisplayName("Consulta de id inexistente responde 404")
    void deveResponder404ParaUsuarioInexistente() throws Exception {
        mvc.perform(get("/api/usuarios/{id}", 999_999).with(admin()))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("ADMIN altera papel e desativa a conta")
    void adminDeveAlterarPapelESituacao() throws Exception {
        Usuario usuario = usuario("promovido@carteira.test");

        mvc.perform(put("/api/usuarios/{id}", usuario.getId()).with(admin()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Conta promovida","email":"promovido@carteira.test","papel":"ADMIN","ativo":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.papel").value("ADMIN"))
                .andExpect(jsonPath("$.ativo").value(false));

        Usuario salvo = usuarioRepository.findById(usuario.getId()).orElseThrow();
        assertThat(salvo.getPapel()).isEqualTo(Papel.ADMIN);
        assertThat(salvo.isAtivo()).isFalse();
    }

    @Test
    @DisplayName("Troca de senha responde 204 e somente a nova senha autentica")
    void adminDeveTrocarSenha() throws Exception {
        Usuario usuario = usuario("troca-senha@carteira.test");

        mvc.perform(put("/api/usuarios/{id}/senha", usuario.getId()).with(admin())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senha\":\"NovaSenha456\"}"))
                .andExpect(status().isNoContent());

        String hash = usuarioRepository.findById(usuario.getId()).orElseThrow().getSenha();
        assertThat(passwordEncoder.matches("NovaSenha456", hash)).isTrue();
        assertThat(passwordEncoder.matches("SenhaAntiga123", hash)).isFalse();
    }

    @Test
    @DisplayName("USER recebe 403 em todas as operações administrativas")
    void userNaoDeveGerenciarUsuarios() throws Exception {
        Usuario usuario = usuario("alvo@carteira.test");
        UserRequestPostProcessor investidor = user("investidor@carteira.test").roles("USER");

        mvc.perform(get("/api/usuarios").with(investidor))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/usuarios").with(investidor).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nome":"Admin falso","email":"falso@carteira.test","senha":"SenhaForte123","papel":"ADMIN"}
                                """))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/usuarios/{id}/senha", usuario.getId()).with(investidor)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"senha\":\"Invadida123\"}"))
                .andExpect(status().isForbidden());

        assertThat(usuarioRepository.existsByEmailIgnoreCase("falso@carteira.test")).isFalse();
    }

    private static UserRequestPostProcessor admin() {
        return user("admin@carteira.test").roles("ADMIN");
    }

    private Usuario usuario(String email) {
        Usuario usuario = new Usuario();
        usuario.setNome("Conta existente");
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode("SenhaAntiga123"));
        return usuarioRepository.save(usuario);
    }
}
