package br.com.carteira.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.carteira.config.JwtProperties;
import br.com.carteira.model.Acao;
import br.com.carteira.model.Mercado;
import br.com.carteira.model.Papel;
import br.com.carteira.model.Usuario;
import br.com.carteira.repository.AcaoRepository;
import br.com.carteira.repository.UsuarioRepository;
import com.jayway.jsonpath.JsonPath;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.transaction.annotation.Transactional;

/** Fluxo real da Aula 13: login, emissão do JWT e validação do token em cada requisição da API. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthApiIntegrationTest {
    private static final String ADMIN = "admin-jwt@carteira.test";
    private static final String INVESTIDOR = "investidor-jwt@carteira.test";
    private static final String SENHA = "SenhaForte123";

    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarioRepository;
    @Autowired AcaoRepository acaoRepository;
    @Autowired PasswordEncoder passwordEncoder;
    @Autowired JwtEncoder jwtEncoder;
    @Autowired JwtProperties jwtProperties;

    @BeforeEach
    void criarContas() {
        usuario(ADMIN, Papel.ADMIN, true);
        usuario(INVESTIDOR, Papel.USER, true);
    }

    @Test
    @DisplayName("POST /api/auth/login devolve um Bearer Token para credenciais válidas")
    void deveEmitirTokenParaCredenciaisValidas() throws Exception {
        mvc.perform(login(ADMIN, SENHA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.tipo").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(900));
    }

    @Test
    @DisplayName("Senha incorreta recebe 401 sem token")
    void deveRecusarSenhaInvalida() throws Exception {
        mvc.perform(login(ADMIN, "SenhaErrada123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.token").doesNotExist())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("E-mail inexistente recebe a mesma resposta da senha incorreta")
    void deveRecusarEmailInexistenteSemRevelarContas() throws Exception {
        mvc.perform(login("ninguem@carteira.test", SENHA))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("E-mail ou senha inválidos."));
    }

    @Test
    @DisplayName("Conta desativada não consegue novo login")
    void deveRecusarLoginDeContaDesativada() throws Exception {
        usuario("inativo-jwt@carteira.test", Papel.USER, false);

        mvc.perform(login("inativo-jwt@carteira.test", SENHA))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Login sem e-mail é rejeitado pela validação com 400")
    void deveValidarCorpoDoLogin() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"\",\"senha\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.erros.email").exists());
    }

    @Test
    @DisplayName("Endpoint da API sem cabeçalho Authorization recebe 401")
    void deveExigirTokenNaApi() throws Exception {
        mvc.perform(get("/api/acoes"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string("WWW-Authenticate", "Bearer"))
                .andExpect(jsonPath("$.detail").value("Autenticação necessária ou token inválido."));
    }

    @Test
    @DisplayName("Token emitido no login libera o acesso à API")
    void deveAcessarApiComTokenEmitido() throws Exception {
        String token = token(INVESTIDOR);

        mvc.perform(get("/api/acoes").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("GET /api/auth/me mostra o e-mail e o papel transportados no token")
    void deveIdentificarUsuarioDoToken() throws Exception {
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(ADMIN))
                .andExpect(jsonPath("$.authorities", hasItem("ROLE_ADMIN")));
    }

    @Test
    @DisplayName("Payload alterado para ROLE_ADMIN invalida a assinatura e recebe 401")
    void deveRecusarTokenAdulterado() throws Exception {
        String[] partes = token(INVESTIDOR).split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(partes[1]), StandardCharsets.UTF_8);
        String payloadForjado = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(payload.replace("ROLE_USER", "ROLE_ADMIN").getBytes(StandardCharsets.UTF_8));

        mvc.perform(get("/api/usuarios")
                        .header("Authorization", "Bearer " + partes[0] + "." + payloadForjado + "." + partes[2]))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token expirado recebe 401")
    void deveRecusarTokenExpirado() throws Exception {
        Instant agora = Instant.now();
        String expirado = assinar(jwtProperties.issuer(), agora.minus(2, ChronoUnit.HOURS),
                agora.minus(1, ChronoUnit.HOURS));

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expirado))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Token assinado com a chave certa, mas de outro emissor, recebe 401")
    void deveRecusarTokenDeOutroEmissor() throws Exception {
        Instant agora = Instant.now();
        String outroEmissor = assinar("outro-sistema", agora, agora.plus(10, ChronoUnit.MINUTES));

        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + outroEmissor))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("USER autenticado recebe 403 na administração de usuários")
    void deveNegarAdministracaoParaUser() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token(INVESTIDOR)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("Usuário autenticado sem permissão para este recurso."));
    }

    @Test
    @DisplayName("ADMIN autenticado por JWT acessa a administração de usuários")
    void devePermitirAdministracaoParaAdmin() throws Exception {
        mvc.perform(get("/api/usuarios").header("Authorization", "Bearer " + token(ADMIN)))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("Autorização horizontal: o token de uma conta não alcança o ativo de outra")
    void deveIsolarCarteiraDeOutraContaNaApi() throws Exception {
        Acao acaoDoAdmin = acaoRepository.save(acao(usuarioRepository.findByEmailIgnoreCase(ADMIN).orElseThrow()));

        mvc.perform(get("/api/acoes/{id}", acaoDoAdmin.getId()).header("Authorization", "Bearer " + token(INVESTIDOR)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/acoes/{id}", acaoDoAdmin.getId()).header("Authorization", "Bearer " + token(ADMIN)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ticker").value("JWTA3"));
    }

    private RequestBuilder login(String email, String senha) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha));
    }

    private String token(String email) throws Exception {
        String corpo = mvc.perform(login(email, SENHA))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    private String assinar(String emissor, Instant emitidoEm, Instant expiraEm) {
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(emissor)
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .subject(ADMIN)
                .claim("authorities", List.of("ROLE_ADMIN"))
                .build();
        return jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }

    private void usuario(String email, Papel papel, boolean ativo) {
        Usuario usuario = new Usuario();
        usuario.setNome("Conta da API");
        usuario.setEmail(email);
        usuario.setSenha(passwordEncoder.encode(SENHA));
        usuario.setPapel(papel);
        usuario.setAtivo(ativo);
        usuarioRepository.save(usuario);
    }

    private Acao acao(Usuario usuario) {
        Acao acao = new Acao();
        acao.setUsuario(usuario);
        acao.setTicker("JWTA3");
        acao.setNomeEmpresa("JWT Participações");
        acao.setMercado(Mercado.BRASIL);
        acao.setMoeda("BRL");
        acao.setCotacaoAtual(new BigDecimal("20.00"));
        acao.setVariacaoPercentual(BigDecimal.ZERO);
        acao.setQuantidade(new BigDecimal("10"));
        acao.setPrecoMedio(new BigDecimal("18.00"));
        acao.setDataHoraCotacao(LocalDateTime.now());
        return acao;
    }
}
