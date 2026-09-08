# Proposta: infraestrutura e banco versionado

## Why

Senhas no código, schema criado automaticamente e dependências instaladas manualmente tornam o projeto inseguro e difícil de reproduzir. O ambiente precisa ser configurável, versionado e executável de forma consistente.

## What

- Externalizar banco, integrações e usuário opcional por variáveis de ambiente.
- Manter `.env.example` sem segredos e ignorar `.env` real.
- Usar Liquibase para criar e evoluir o schema.
- Manter Hibernate em `ddl-auto=validate`.
- Containerizar aplicação e PostgreSQL com Docker Compose.
- Persistir PostgreSQL em volume e aguardar healthcheck.

## Impact

Arquivos de configuração Spring, changelogs, `pom.xml`, Dockerfile, Compose, `.gitignore`, `.dockerignore`, README e testes de migration.

## Segurança

A imagem não recebe `.env`, a aplicação final executa sem root e perfis externos não possuem senha padrão.

## Critério de conclusão

Um banco vazio deve ser criado integralmente pelas migrations; reinicialização não deve reaplicar changeSets nem perder dados.
