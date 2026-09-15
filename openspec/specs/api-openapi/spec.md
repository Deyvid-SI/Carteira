# API e OpenAPI Specification

## Purpose

Fornecer contrato navegável e respostas REST padronizadas para integração e avaliação.

## Requirements

### Requirement: Contrato OpenAPI

O sistema SHALL gerar um documento OpenAPI e uma interface Swagger UI com todos os endpoints REST, acessíveis somente a usuários com papel `ADMIN`.

#### Scenario: Usuário comum tenta consultar o contrato

- GIVEN um usuário autenticado com papel `USER`
- WHEN `/v3/api-docs` ou `/swagger-ui.html` é solicitado
- THEN o acesso é negado com HTTP 403

#### Scenario: Consulta do contrato

- GIVEN um usuário autenticado com papel `ADMIN`
- WHEN `/v3/api-docs` é solicitado
- THEN um documento OpenAPI contendo `/acoes`, `/corretoras`, `/negociacoes` e `/dashboard/dados` é retornado

#### Scenario: Navegação do usuário

- WHEN a barra superior é renderizada
- THEN nenhum link para OpenAPI ou Swagger é exibido

### Requirement: Erros centralizados

O sistema SHALL representar validações, recursos ausentes, regras de negócio e falhas externas com status HTTP adequado e `ProblemDetail`.

#### Scenario: API externa indisponível

- WHEN uma integração falha por indisponibilidade ou limite
- THEN a API retorna HTTP 502
- AND informa o nome do serviço sem expor segredos
