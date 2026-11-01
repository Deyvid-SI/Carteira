# Autenticação Specification

## Purpose

Proteger dados e operações da carteira por uma sessão de usuário validada no portal e por JWT na API REST, com autorização baseada em papéis.

## Requirements

### Requirement: Cadastro validado

O sistema SHALL permitir cadastro com nome, e-mail único e senha de 8 a 72 caracteres contendo letra maiúscula, minúscula e número.

#### Scenario: Cadastro válido

- GIVEN um e-mail ainda não utilizado
- WHEN o usuário informa dados válidos e duas senhas iguais
- THEN a senha é armazenada como hash BCrypt
- AND o usuário pode autenticar-se

#### Scenario: E-mail duplicado

- GIVEN um e-mail já cadastrado
- WHEN um novo cadastro usa o mesmo e-mail sem diferenciar maiúsculas
- THEN o sistema rejeita o cadastro com mensagem clara

### Requirement: Controle de acesso

O sistema SHALL exigir autenticação para dashboard, tabelas e endpoints de negócio.

#### Scenario: Visitante acessa área protegida

- GIVEN um visitante sem sessão
- WHEN ele solicita uma página de gestão
- THEN é direcionado ao login

#### Scenario: Credenciais inválidas

- GIVEN uma combinação de e-mail e senha incorreta
- WHEN o login é enviado
- THEN nenhuma sessão autenticada é criada
- AND a tela informa que as credenciais são inválidas

### Requirement: Login da API com JWT

O sistema SHALL autenticar clientes da API em `POST /api/auth/login` e emitir um JWT HS256 com `iss`, `sub`, `iat`, `exp` e `authorities`, sem dados sigilosos no payload. Todas as demais rotas `/api/**` SHALL exigir `Authorization: Bearer <token>` e nenhuma sessão HTTP SHALL ser criada.

#### Scenario: Credenciais válidas

- GIVEN uma conta ativa com senha BCrypt
- WHEN o cliente envia e-mail e senha corretos
- THEN recebe `200` com `token`, `tipo` igual a `Bearer` e `expiresIn` em segundos

#### Scenario: Credenciais inválidas ou conta desativada

- GIVEN senha incorreta, e-mail inexistente ou conta com `ativo=false`
- WHEN o login é enviado
- THEN a resposta é `401` com a mesma mensagem genérica, sem revelar quais e-mails existem

#### Scenario: Token ausente, adulterado, expirado ou de outro emissor

- GIVEN uma requisição a `/api/**` sem token válido
- WHEN ela chega à API
- THEN a resposta é `401` em `ProblemDetail` com o cabeçalho `WWW-Authenticate: Bearer`

#### Scenario: Configuração insegura

- GIVEN `JWT_SECRET` ausente ou com menos de 32 bytes
- WHEN a aplicação inicia
- THEN a inicialização falha com mensagem explicando a exigência

### Requirement: Autorização por papéis

O sistema SHALL aplicar RBAC com os papéis `USER` e `ADMIN` por meio de `@EnableMethodSecurity` e `@PreAuthorize`, e SHALL restringir cada conta aos dados da própria carteira.

#### Scenario: Usuário sem papel administrativo

- GIVEN um token válido com `ROLE_USER`
- WHEN o cliente acessa `/api/usuarios`
- THEN a resposta é `403`

#### Scenario: Carteira de outra conta

- GIVEN um token válido da conta B
- WHEN o cliente solicita uma ação pertencente à conta A
- THEN a resposta é `404`, sem revelar dados da conta A

### Requirement: Gerenciamento administrativo de usuários

O sistema SHALL permitir que somente `ADMIN` liste, consulte, cadastre, altere papel/situação e redefina a senha de contas, sempre codificando a senha com BCrypt e sem expor senha ou hash nas respostas. Contas SHALL ser desativadas em vez de excluídas.

#### Scenario: Cadastro administrativo

- GIVEN um administrador autenticado
- WHEN cadastra uma conta com e-mail ainda não utilizado
- THEN a resposta é `201` sem campo de senha
- AND o banco armazena apenas o hash BCrypt

#### Scenario: Troca de senha

- GIVEN um administrador autenticado
- WHEN define uma nova senha para uma conta
- THEN a resposta é `204`
- AND somente a nova senha autentica
