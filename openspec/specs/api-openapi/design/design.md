# Design: API REST e OpenAPI

## Superfície REST

```text
/acoes          cadastro, consulta, cotação e posição
/corretoras     cadastro validado e consultas
/negociacoes    compra, venda e histórico
/dashboard      leitura consolidada
```

Controllers recebem DTOs validados, delegam regras aos services e convertem entidades em DTOs de resposta. Entidades JPA não são serializadas diretamente.

## Erros

`ApiExceptionHandler` converte exceções em RFC 7807:

- 400 para validação estrutural;
- 404 para recurso ausente;
- 422 para regra de negócio;
- 502 para falha de integração externa.

Campos adicionais incluem timestamp, instância e erros por campo quando aplicável.

## Contrato

Springdoc inspeciona controllers e anotações. `OpenApiConfig` define metadados e o esquema Bearer JWT; `springdoc.paths-to-match` limita o contrato a `/api/**`. O contrato continua gerado em runtime para não divergir do código.

## Controle de acesso

Matchers de Swagger, `/v3/api-docs/**` e H2 Console exigem `ROLE_ADMIN`. Endpoints de negócio ficam em `/api/**` e exigem Bearer Token JWT; o portal usa sessão.

## Testes

MockMvc verifica contrato para administrador, HTTP 403 para usuário comum e presença dos principais caminhos.
