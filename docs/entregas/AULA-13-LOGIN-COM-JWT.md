# Aula 13 — Login e proteção da API com JWT

Entrega do projeto **Carteira de Ações** para a Aula 13 do repositório `suporteos2025`. A aula pede que a solução seja reproduzida no tema do próprio projeto, adaptando os nomes de domínio sem mudar os princípios de segurança. As adaptações estão indicadas na última seção.

## Como foi implementado

| Ponto de parada da aula | Onde está no projeto |
|---|---|
| 1. Dependências | `pom.xml`: `spring-boot-starter-security`, `spring-boot-starter-oauth2-resource-server` e `spring-security-test` |
| 2. Usuário versionado | Entidade `Usuario` com enum `Papel` (`ADMIN`, `USER`, persistido com `EnumType.STRING`); tabela `usuarios` criada pelo Liquibase; `UsuarioRepository.findByEmailIgnoreCase` |
| 3. Variáveis de ambiente | `JWT_SECRET`, `JWT_EXPIRATION` e `JWT_ISSUER` em `application.yml`, `.env.example` e `compose.yaml`; o `.env` está no `.gitignore` |
| 4. Spring Security | `UsuarioDetailsService`; `JwtProperties` valida a chave (≥ 32 bytes), a validade e o emissor; `SecurityConfig` com cadeia stateless em `/api/**`, BCrypt, `JwtEncoder`, `JwtDecoder` HS256 e validador de emissor |
| 5. Login e token | `LoginRequest`, `TokenResponse`, `AuthService`; `POST /api/auth/login` e `GET /api/auth/me` em `AuthRestController`; respostas `401` e `403` em JSON (`RestAuthenticationEntryPoint` e `RestAccessDeniedHandler`) |
| 6. Administrador inicial | `DataInitializer`, criado somente pelas variáveis `DEMO_USER_*` e com a senha codificada em BCrypt antes de salvar |
| 12. Postman | `postman/Carteira-Acao.postman_collection.json` e o environment `postman/Carteira-Acao-Local.postman_environment.json`; o login salva o token com `pm.environment.set` |
| 13. Testes | `AuthApiIntegrationTest` (16 testes com o JWT real) e `SecurityFlowTest` |

## Execução pelo Docker Compose (seção 11.2)

Executada em 26/11/2026 com `docker compose up --build`, num projeto Compose separado (`-p carteira-acao-teste`), com volume e portas próprios, para não tocar nos dados locais. O ambiente foi removido no final com `docker compose down --volumes`.

| Verificação | Resultado |
|---|---|
| Build da imagem (Maven dentro do container) e subida dos serviços | `aplicacao` e `postgres` em execução |
| Ordem de inicialização | O PostgreSQL ficou `healthy` antes de a aplicação iniciar (`depends_on: service_healthy`) |
| Perfil e banco | Perfil `dev` com PostgreSQL 17; aplicação pronta em cerca de 20 s |
| Liquibase num banco vazio | Os 7 changesets aplicados (`001` a `007`), seguidos da validação do Hibernate |
| Experimentos das Aulas 13 e 14 contra os containers, com `JWT_EXPIRATION=10s` | 19 de 19 com o resultado esperado |
| Recriação do container da aplicação | Os dados no volume do PostgreSQL continuaram lá: a conta criada antes ainda entrou |
| Coleção do Postman contra os containers, executada duas vezes seguidas | 31 requisições, **31 de 31 `pm.test`** nas duas execuções |
| Portal com a conta `ADMIN` | Dashboard, gestão e `/admin` com `200`; formulários de cadastro visíveis |
| Portal com a conta `USER` | Dashboard e gestão com `200` e aviso "Somente consulta", sem formulários; `/admin` com `403` |

A primeira execução da coleção mostrou que ela só funcionava num banco vazio: o cadastro de `PETR4` recebeu `422` porque o ticker já existia na conta. A coleção foi ajustada para poder ser repetida: o e-mail do usuário de teste é único a cada execução, os cadastros de dados fixos aceitam `422` ("já cadastrado"), os ids são lidos das respostas e todas as 31 requisições têm `pm.test`.

## Experimentos obrigatórios (seção 12.4)

Executados em 24/11/2026 contra a aplicação real (`.jar` gerado pelo `./mvnw`), com banco em memória e `JWT_EXPIRATION=10s`. O roteiro está em `postman/`, na pasta **Experimentos (Aulas 13 e 14)**.

| # | Experimento | Esperado | Obtido |
|---|---|:---:|:---:|
| 1 | Acessar `/api/acoes` sem o cabeçalho `Authorization` | `401` | `401` |
| 2 | Login com senha incorreta | `401` | `401` |
| 3 | Alterar um caractere da assinatura do token | `401` | `401` |
| 4 | Usar o token correto em `/api/auth/me` | `200` | `200` |
| 5 | Conta `USER` tenta uma operação restrita a `ADMIN` (`POST /api/acoes`) | `403` | `403` |
| 6 | `JWT_EXPIRATION=10s`: o mesmo token usado 12 s depois | `401` | `401` |

Token emitido no experimento 4:

- Cabeçalho: `{"typ": "JWT", "alg": "HS256"}`
- Payload: `{"iss": "carteira-acao", "sub": "admin@experimento.local", "iat": 1795562421, "exp": 1795562431, "authorities": ["ROLE_ADMIN"]}`
- Validade observada: `exp - iat = 10 s`

**O que o experimento 6 revelou.** Na primeira execução, o token ainda foi aceito 12 s depois (`200`). O `JwtTimestampValidator` padrão do Spring Security aceita até **60 s** de diferença de relógio. Como aqui o mesmo servidor emite e valida o token, essa folga foi zerada (`new JwtTimestampValidator(Duration.ZERO)`), seguindo um ciclo de TDD: primeiro o teste `deveRecusarTokenLogoDepoisDeExpirar` falhou com `200`, depois passou. Na segunda execução, o resultado foi o esperado.

O log da aplicação foi conferido depois dos experimentos: nenhuma senha e nenhum token completo apareceu nele.

## Checklist de segurança (seção 14)

| Item | Situação | Evidência |
|---|:---:|---|
| `.env` continua ignorado pelo Git | ok | `.gitignore` (`.env` e `.env.*`, exceto `.env.example`) |
| Nenhuma senha escrita em Java ou em arquivo versionado | ok | O administrador inicial vem de variáveis de ambiente; a chave dos testes é exclusiva do ambiente de teste |
| `JWT_SECRET` com pelo menos 32 bytes e imprevisível | ok | `JwtProperties` recusa chaves menores (testado com 31 e 32 bytes); o `.env.example` deixa a chave vazia para obrigar a gerar uma |
| Coluna `senha` com hash BCrypt | ok | `UsuarioApiIntegrationTest` confere o prefixo `$2` e `passwordEncoder.matches` |
| O login é a única rota pública | ok | `SecurityFlowTest.loginDeveSerAUnicaRotaPublica`; não existe cadastro aberto |
| Requisições sem token recebem `401` | ok | Experimento 1 e `AuthApiIntegrationTest.deveExigirTokenNaApi` |
| Tokens adulterados ou expirados recebem `401` | ok | Experimentos 3 e 6; testes de token adulterado, expirado, recém-expirado e de outro emissor |
| A aplicação não imprime senhas nem tokens completos | ok | Busca no log da aplicação após os experimentos: 0 ocorrências |
| Produção utiliza HTTPS | não se aplica | O projeto roda localmente e no Docker Compose, sem ambiente de produção publicado |
| Chaves diferentes por ambiente | ok | Testes usam `chave-exclusiva-para-testes-da-carteira-...`; desenvolvimento usa a `JWT_SECRET` do `.env` |

## Atividade avaliativa (seção 15)

1. **Usuário persistido e versionado pelo Liquibase:** tabela `usuarios` no changeset `001`, com e-mail único, hash da senha, papel e situação.
2. **Login JWT seguindo o fluxo da aula:** `POST /api/auth/login` autentica pelo `AuthenticationManager` e só depois emite o token assinado.
3. **Endpoints de negócio protegidos:** toda rota `/api/**`, exceto o login, exige `Authorization: Bearer`.
4. **Pelo menos quatro testes de integração de segurança:** são 16 em `AuthApiIntegrationTest`, além de `UsuarioApiIntegrationTest`, `AdminWebIntegrationTest` e `SecurityFlowTest`.
5. **Coleção do Postman:** `postman/Carteira-Acao.postman_collection.json` e o environment `postman/Carteira-Acao-Local.postman_environment.json`.
6. **Por que o JWT não deve conter dados sigilosos.** O cabeçalho e o payload do JWT são apenas codificados em Base64URL, não criptografados: qualquer pessoa que tenha o token consegue lê-los, como mostra o payload acima, decodificado sem a chave. A assinatura HS256 garante que o conteúdo não foi alterado, mas não esconde nada. Por isso o token deste projeto carrega só o emissor, o e-mail da conta, as datas e o papel; senha, hash, CPF ou valores da carteira nunca entram nele.
7. **Diferença observada entre `401` e `403`.** O `401 Unauthorized` apareceu quando a aplicação não conseguiu identificar quem fazia a requisição: sem token, com senha errada, com assinatura alterada ou com o token vencido (experimentos 1, 2, 3 e 6). O `403 Forbidden` apareceu quando a identidade era válida, mas o papel não permitia a operação: a conta `USER`, com um token correto, tentou cadastrar uma ação (experimento 5). Em resumo: `401` é "não sei quem é você"; `403` é "sei quem é você, mas você não pode fazer isso".

## Adaptações ao tema do projeto

| Na aula | No projeto | Motivo |
|---|---|---|
| `username` e `password` | `email` e `senha` | O login do portal já usava o e-mail |
| `AdminInitializer` só no perfil `dev`, com `APP_ADMIN_*` | `DataInitializer` com `DEMO_USER_*`, ligado por variável | O perfil padrão do projeto usa H2; sem cadastro público, o administrador inicial também precisa existir fora do perfil `dev` |
| `app.jwt.*` em `application-dev.properties` | `app.jwt.*` em `application.yml` | Vale para todos os perfis, sempre lido do ambiente |
| `@ActiveProfiles("test")` | `src/test/resources/application.yml` | Os testes já tinham configuração isolada com H2 em memória |
| Grupos e produtos | Ações, corretoras, negociações e usuários | Domínio do projeto |
