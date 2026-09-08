# Infraestrutura e banco versionado Specification

## Purpose

Padronizar configuração segura, evolução do banco e execução containerizada da aplicação.

## Requirements

### Requirement: Configuração externa

O sistema SHALL aceitar URL, usuário e senha do banco por DB_URL, DB_USERNAME e DB_PASSWORD, sem senha-padrão nos perfis PostgreSQL e MySQL.

#### Scenario: Senha ausente no perfil dev

- WHEN o perfil dev é iniciado sem DB_PASSWORD
- THEN a inicialização falha sem recorrer a uma senha conhecida

### Requirement: Arquivo de ambiente seguro

O projeto SHALL ignorar .env e SHALL manter somente .env.example com valores falsos ou públicos.

#### Scenario: Preparação do ambiente local

- WHEN o desenvolvedor copia .env.example para .env
- THEN os nomes das variáveis necessárias estão documentados
- AND o arquivo local permanece fora do versionamento e do contexto Docker

### Requirement: Schema versionado

O sistema SHALL executar o master changelog do Liquibase antes do JPA e SHALL usar o Hibernate apenas com ddl-auto=validate.

#### Scenario: Inicialização de banco vazio

- WHEN a aplicação inicia com um banco vazio
- THEN o Liquibase cria usuários, corretoras, ações e históricos
- AND cria constraints, chave estrangeira e índice
- AND o Hibernate valida o schema resultante

### Requirement: Reinicialização idempotente

O sistema SHALL registrar changeSets executados e não deverá executá-los novamente em um banco já migrado.

#### Scenario: Segunda inicialização

- WHEN a aplicação reinicia sobre um banco já migrado
- THEN nenhum changeSet é reaplicado
- AND os dados existentes permanecem disponíveis

### Requirement: Imagem segura da aplicação

O projeto SHALL construir uma imagem multi-stage com Java 17 e SHALL executar o processo final como usuário sem privilégios de root.

#### Scenario: Construção da imagem

- WHEN o Dockerfile é usado para construir a aplicação
- THEN Maven e o código-fonte permanecem somente na etapa de build
- AND a etapa final contém o JRE e o JAR executado pelo usuário spring

### Requirement: Ambiente composto

O Compose SHALL executar aplicação e PostgreSQL em serviços separados, aguardar o healthcheck do banco e persistir os dados em volume nomeado.

#### Scenario: Inicialização dos containers

- WHEN docker compose up é executado com as variáveis obrigatórias
- THEN o PostgreSQL fica saudável antes da aplicação
- AND a aplicação conecta pelo hostname interno postgres
- AND os dados são gravados no volume postgres_data
