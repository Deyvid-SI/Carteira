# Proposta: implementar carteira de ações

## Why

O repositório continha somente uma classe Java de exemplo e precisava atender ao trabalho de programação: aplicação Spring Boot em camadas, integrações externas reais, persistência relacional, autenticação, gráficos, tabelas e documentação.

## What Changes

- Substituir o programa de console por aplicação Spring Boot full stack.
- Criar autenticação e cadastro de usuário validados.
- Implementar gestão REST e web de corretoras e ações.
- Integrar BrasilAPI, ViaCEP, brapi e Alpha Vantage por gateways isolados.
- Registrar histórico de cotação e produzir dashboard responsivo.
- Gerar OpenAPI/Swagger, coleção Postman e documentação de execução.
- Adicionar H2 padrão e perfis MySQL/PostgreSQL.

## Capabilities

- `autenticacao`
- `corretoras`
- `acoes`
- `dashboard`
- `api-openapi`

## Impact

O projeto passa a exigir Java 17, Maven e acesso à internet durante consultas externas. Tickers além das amostras gratuitas dependem das chaves configuradas no ambiente.
