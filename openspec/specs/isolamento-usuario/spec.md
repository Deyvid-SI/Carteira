# Isolamento por Usuário Specification

## Purpose

Garantir que cada conta possua uma carteira privada e independente.

## Requirements

### Requirement: Carteira vazia no cadastro

O sistema SHALL criar contas sem herdar ações, corretoras, negociações, histórico ou consolidações de outras contas.

#### Scenario: Primeiro acesso de uma conta nova

- GIVEN que já existem dados em outra conta
- WHEN um novo usuário cria sua conta e entra no sistema
- THEN seu dashboard apresenta zero ativos e zero corretoras
- AND suas telas de ativos, negociações, histórico e Imposto de Renda não exibem registros alheios

### Requirement: Propriedade dos dados

O sistema SHALL associar ações e corretoras ao usuário autenticado e derivar a propriedade de negociações e cotações pela ação correspondente.

#### Scenario: Cadastro autenticado

- GIVEN um usuário autenticado
- WHEN ele cadastra uma ação ou corretora
- THEN o registro é associado à sua conta

### Requirement: Consultas delimitadas pela conta

O sistema SHALL filtrar por usuário todas as consultas de ações, corretoras, negociações, dashboard, histórico e relatórios fiscais.

#### Scenario: Tentativa de consultar registro alheio

- GIVEN que um identificador pertence a outra conta
- WHEN o usuário solicita esse registro pela interface ou API
- THEN o sistema responde como recurso não encontrado
- AND nenhum dado do proprietário é revelado

### Requirement: Unicidade por conta

O sistema SHALL permitir o mesmo ticker ou CNPJ em contas diferentes e rejeitar duplicidade somente dentro da mesma conta.

#### Scenario: Duas carteiras com o mesmo ticker

- GIVEN duas contas independentes
- WHEN ambas cadastram `PETR4`
- THEN cada cadastro é aceito uma vez em sua própria conta
- AND alterações em uma posição não afetam a outra
