# Tasks: infraestrutura e banco versionado

## Variáveis de ambiente

- [x] Externalizar URL, usuário e senha do banco.
- [x] Remover senhas padrão dos perfis externos.
- [x] Criar `.env.example` seguro.
- [x] Ignorar `.env` e variações locais.
- [x] Documentar diferença entre Spring e Compose ao ler `.env`.

## Liquibase

- [x] Adicionar dependência Liquibase.
- [x] Criar master changelog.
- [x] Criar migration inicial do schema.
- [x] Criar migration separada de negociações.
- [x] Configurar Hibernate com `ddl-auto=validate`.
- [x] Executar o mesmo changelog nos testes.
- [x] Preservar o H2 existente durante a transição.

## Docker

- [x] Criar `.dockerignore` sem segredos.
- [x] Criar Dockerfile multi-stage Java 17.
- [x] Executar imagem final como usuário sem root.
- [x] Criar Compose com aplicação e PostgreSQL 17.
- [x] Configurar healthcheck, rede, portas e volume.
- [x] Documentar comandos de build, start, stop e remoção.

## Verificação

- [x] Validar migrations em banco vazio e existente.
- [x] Validar sintaxe do Compose.
- [x] Gerar JAR executável.
- [x] Construir a imagem com o Docker Engine local.
- [x] Iniciar PostgreSQL e aplicação pelo Compose.
- [x] Confirmar PostgreSQL saudável, 5 changeSets aplicados e login HTTP acessível.
