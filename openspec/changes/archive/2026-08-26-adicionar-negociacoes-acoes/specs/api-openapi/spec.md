# Delta: acesso à documentação OpenAPI

## MODIFIED Requirements

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

- WHEN a barra lateral é renderizada
- THEN nenhum link para OpenAPI ou Swagger é exibido
