# Proposta: API REST e contrato OpenAPI

## Why

Integrações e avaliação técnica precisam de endpoints previsíveis, erros padronizados e contrato navegável, sem expor ferramentas de desenvolvimento ao usuário comum.

## What

- Disponibilizar endpoints REST para ações, corretoras, negociações e dashboard.
- Gerar contrato OpenAPI automaticamente.
- Restringir Swagger e documentos API ao papel `ADMIN`.
- Padronizar erros com `ProblemDetail`.
- Manter coleção Postman com autenticação configurável.

## Impact

Controllers REST, `OpenApiConfig`, `ApiExceptionHandler`, Spring Security, testes MockMvc e coleção Postman.

## Segurança

OpenAPI não aparece na navegação superior. Usuário `USER` recebe HTTP 403; visitante sem credenciais recebe desafio de autenticação.

## Critério de conclusão

O contrato deve refletir os endpoints reais e nenhuma mensagem de erro pode expor senha, token ou payload sensível de terceiros.
