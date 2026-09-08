# Design: infraestrutura e banco versionado

## Configuração externa

```text
Sistema/IDE/Compose → variáveis de ambiente → placeholders Spring → DataSource
```

`DB_PASSWORD` não possui padrão nos perfis PostgreSQL e MySQL. O perfil H2 local usa configuração sem segredo.

## Liquibase

```text
DataSource conecta
  → Liquibase lê DATABASECHANGELOG
  → executa changeSets pendentes
  → Hibernate valida entidades
  → aplicação inicia
```

O master inclui migrations em ordem. ChangeSet executado é imutável; toda evolução cria novo arquivo. Testes usam H2 em modo PostgreSQL e o mesmo master.

## Containers

O Dockerfile possui duas etapas:

1. Maven + JDK 17 compilam o JAR;
2. JRE 17 executa somente o JAR como usuário `spring`.

Compose cria rede, aplicação, PostgreSQL 17 e volume `postgres_data`. O hostname interno do banco é `postgres`, nunca `localhost`. No computador, a aplicação é publicada em `18080` e o PostgreSQL em `5433`, evitando conflito com serviços locais nas portas convencionais.

## Inicialização

Healthcheck `pg_isready` controla a dependência. Após banco saudável, Spring recebe perfil `dev`, URL, usuário e senha por environment.

## Persistência e recuperação

`docker compose down` preserva o volume. `down --volumes` é destrutivo e deve ser usado apenas para recriação consciente.

## Testes

O teste de migration confirma tabelas, changeSets e compatibilidade do Hibernate. A validação integrada constrói a imagem, inicia os dois serviços, confirma o healthcheck do PostgreSQL, os cinco changeSets do Liquibase e o acesso HTTP à tela de login.
