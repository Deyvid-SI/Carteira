# Aula 14 — Controle de acesso baseado em papéis

Entrega do projeto **Carteira de Ações** para a Aula 14 do repositório `suporteos2025`.

## Matriz de acesso (seção 4)

Definida antes da implementação, como a aula pede, e conferida pelos testes e pelos experimentos abaixo.

| Recurso ou operação | Público | `USER` | `ADMIN` |
|---|:---:|:---:|:---:|
| Login (`/login` e `POST /api/auth/login`) | Sim | Sim | Sim |
| `GET /api/auth/me` | Não | Sim | Sim |
| Consultar ações, corretoras, negociações, dashboard, histórico e IR da própria carteira | Não | Sim | Sim |
| Cadastrar ações e corretoras | Não | Não | Sim |
| Alterar cotação e posição de ações | Não | Não | Sim |
| Registrar compras e vendas | Não | Não | Sim |
| Gerenciar usuários (`/api/usuarios` e área `/admin`) | Não | Não | Sim |
| Swagger, OpenAPI e console H2 | Não | Não | Sim |

O domínio do projeto não tem exclusões de ações, corretoras ou negociações, e contas são desativadas em vez de excluídas (seção 9). Por isso a linha "excluir" da matriz da aula não tem correspondente aqui.

## Como foi implementado

| Ponto de parada | Implementação |
|---|---|
| 1. Segurança de métodos | `@EnableMethodSecurity` em `SecurityConfig`; a cadeia HTTP mantém só `/api/auth/login` público e `anyRequest().authenticated()` |
| 2. Escritas só para ADMIN | `@PreAuthorize("hasRole('ADMIN')")` nos métodos `POST` e `PUT` de `AcaoRestController`, `CorretoraRestController` e `NegociacaoRestController`, e nos envios de formulário de `GestaoWebController`; os `GET` continuam abertos a qualquer conta autenticada |
| 3. Contratos seguros | `UsuarioCreateRequest`, `UsuarioUpdateRequest`, `AlterarSenhaRequest` e `UsuarioResponse`, este sem campo de senha |
| 4. Serviço de usuários | `UsuarioService`: senha codificada antes de salvar, e-mail único, desativação e troca de senha |
| 5. API administrativa | `UsuarioRestController` em `/api/usuarios`, com `@PreAuthorize` na classe: listar, consultar, cadastrar, alterar e definir senha. Não há `DELETE` |

## Onde ficou cada `@PreAuthorize` e por quê (atividade 7)

- **Métodos de escrita dos controllers de negócio:** a anotação fica no próprio método, ao lado do `@PostMapping` ou `@PutMapping`. Quem lê o endpoint vê na hora que ele exige `ADMIN`, e a regra continua valendo mesmo que a URL mude. Os métodos `GET` ficam sem anotação de propósito, liberados pela regra `anyRequest().authenticated()`.
- **`UsuarioRestController` e `AdminWebController`:** a anotação fica na classe, porque todos os métodos são administrativos, e assim nenhum método novo nasce desprotegido.
- **Portal (`GestaoWebController`):** a mesma anotação nos envios de formulário, porque de nada adiantaria bloquear a API se o portal continuasse aceitando o cadastro. A tela também esconde formulários e botões da conta `USER`, mas isso é só usabilidade: quem protege é o `@PreAuthorize`, e um envio direto recebe `403`.
- **Swagger, OpenAPI e H2:** a regra fica na URL (`requestMatchers(...).hasRole("ADMIN")`), porque não são controllers da aplicação.

## Testes com Postman (seção 10)

Executados em 24/11/2026 contra a aplicação real, com banco em memória. O roteiro está em `postman/`, na pasta **Experimentos (Aulas 13 e 14)**.

| Seção | Operação | Esperado | Obtido |
|---|---|:---:|:---:|
| 10.1 | `ADMIN` cadastra a conta `USER` aluno (resposta sem senha e sem hash) | `201` | `201` |
| 10.2 | `USER`: `GET /api/acoes` | `200` | `200` |
| 10.2 | `USER`: `POST /api/acoes` | `403` | `403` |
| 10.2 | `USER`: `PUT /api/acoes/{id}/posicao` | `403` | `403` |
| 10.2 | `USER`: `POST /api/negociacoes` | `403` | `403` |
| 10.2 | `USER`: `GET /api/usuarios` | `403` | `403` |
| 10.3 | `ADMIN`: `GET /api/usuarios` | `200` | `200` |
| 10.3 | `ADMIN`: `POST /api/acoes` com corpo válido | `201` | `201` |
| 10.3 | `ADMIN`: `PUT /api/usuarios/{id}` com `ativo=false` | `200` | `200` |
| 10.3 | Novo login da conta desativada | `401` | `401` |
| 10.4 | `ADMIN`: `PUT /api/usuarios/{id}/senha` | `204` | `204` |
| 10.4 | Senha antiga depois da troca | `401` | `401` |
| 10.4 | Senha nova depois da troca | `200` | `200` |

## Checklist (seção 14)

| Item | Situação | Evidência |
|---|:---:|---|
| `@EnableMethodSecurity` está ativo | ok | `SecurityConfig` |
| O login permanece público | ok | `SecurityFlowTest.loginDeveSerAUnicaRotaPublica` |
| As demais rotas exigem JWT | ok | `AuthApiIntegrationTest.deveExigirTokenNaApi` |
| Somente `ADMIN` altera dados de negócio | ok | `CarteiraApiIntegrationTest.userSomenteConsulta` e `GestaoWebIntegrationTest.userSomenteConsultaNoPortal` |
| Somente `ADMIN` gerencia usuários | ok | `UsuarioApiIntegrationTest.userNaoDeveGerenciarUsuarios` e `AdminWebIntegrationTest` |
| `USER` consegue executar consultas | ok | Os mesmos testes conferem `200` nas consultas |
| `UsuarioResponse` não contém senha | ok | `UsuarioApiIntegrationTest.adminDeveCadastrarUsuarioSemExporSenha` |
| Toda senha passa pelo `PasswordEncoder` | ok | `UsuarioServiceTest` e o teste de hash BCrypt |
| Usuários desativados em vez de removidos | ok | Não existe `DELETE` de usuário; experimento 10.3 |
| Testes cobrem acessos permitidos e negados | ok | `200`, `401` e `403` nos testes de integração |

## Atividade avaliativa (seção 13)

1. **Pelo menos dois papéis:** `ADMIN` e `USER` (enum `Papel`).
2. **Matriz de acesso antes de programar:** seção "Matriz de acesso" deste documento e do `README.md`.
3. **Escrita restrita a um papel administrativo:** cadastro e alteração de ações, corretoras e negociações, na API e no portal, só para `ADMIN`.
4. **Gestão de usuários sem expor hashes:** `/api/usuarios` e a área `/admin` usam `UsuarioResponse`, sem campo de senha.
5. **Testes que demonstram `200`, `401` e `403`:** `AuthApiIntegrationTest`, `UsuarioApiIntegrationTest`, `CarteiraApiIntegrationTest`, `GestaoWebIntegrationTest` e `AdminWebIntegrationTest`.
6. **Regra de autorização horizontal do domínio.** Cada ação e corretora pertence a uma conta (`usuario_id`), e os serviços consultam sempre pelo usuário autenticado (`UsuarioAtualService`). Assim, uma conta nunca enxerga nem altera a carteira de outra: o token da conta B recebe `404` ao pedir uma ação da conta A, sem revelar que ela existe (`AuthApiIntegrationTest.deveIsolarCarteiraDeOutraContaNaApi` e `UsuarioIsolationIntegrationTest`). A regra vale também para o administrador.
7. **Justificativa de cada `@PreAuthorize`:** seção "Onde ficou cada `@PreAuthorize` e por quê".

## Discussão de segurança (seção 12)

- **Alteração do próprio papel (12.1):** o projeto foi além da versão didática. O administrador não consegue rebaixar nem desativar a própria conta, e o sistema recusa rebaixar ou desativar o último administrador ativo.
- **Tokens já emitidos (12.2):** desativar a conta impede novos logins (experimento 10.3), mas um token já emitido vale até `exp`. Por isso a validade padrão é curta (60 min) e o projeto não aceita folga depois da expiração.
- **Auditoria (12.3):** criação de contas, alteração de cadastro e de papel, desativação, reativação e troca de senha ficam registradas em `atividades_admin`, sem senha e sem token, e aparecem no painel `/admin`.
- **Proteção contra abuso (12.4):** rate limiting no login e HTTPS ficam para a implantação em produção, que este projeto acadêmico não tem.
