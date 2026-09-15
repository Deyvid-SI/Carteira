# Negociações Specification

## Purpose

Registrar compras e vendas com consistência financeira, histórico auditável e atualização automática das posições.

## Requirements

### Requirement: Registro de compra

O sistema SHALL registrar uma compra e atualizar quantidade e preço médio do ativo na mesma transação.

#### Scenario: Compra pelo preço informado e sem taxa manual

- GIVEN 10 unidades com preço médio 100 e cotação atual 120
- WHEN são compradas 5 unidades pelo preço informado de 130
- THEN o saldo passa a 15 unidades
- AND o preço executado é 130
- AND a taxa registrada automaticamente é zero
- AND o desembolso registrado é 650
- AND o novo preço médio é 110
- AND o resultado realizado da compra é zero

### Requirement: Registro de venda

O sistema SHALL registrar uma venda, reduzir o saldo e calcular o resultado realizado com taxa automática zero, sem recalcular o preço médio das unidades restantes.

#### Scenario: Venda parcial com lucro

- GIVEN 10 unidades com preço médio 100 e cotação atual 120
- WHEN são vendidas 4 unidades pelo preço informado de 130
- THEN o saldo passa a 6 unidades
- AND o preço médio permanece 100
- AND o preço executado é 130
- AND a taxa registrada automaticamente é zero
- AND o valor recebido é 520
- AND o resultado realizado é 120

#### Scenario: Venda abaixo do preço médio

- GIVEN uma posição com preço médio 100
- WHEN parte da posição é vendida pelo preço informado de 70
- THEN o resultado realizado registra o prejuízo
- AND o preço médio das unidades restantes permanece 100

#### Scenario: Encerramento da posição

- GIVEN uma venda com quantidade igual ao saldo disponível
- WHEN a operação é concluída
- THEN o saldo e o preço médio consolidados passam a zero

### Requirement: Proteção de saldo

O sistema SHALL rejeitar uma venda cuja quantidade supere a posição disponível.

#### Scenario: Venda sem quantidade suficiente

- GIVEN saldo de 10 unidades
- WHEN uma venda de 11 unidades é solicitada
- THEN a operação retorna uma regra de negócio violada
- AND nem a posição nem o histórico são alterados

### Requirement: Validação financeira e temporal

O sistema SHALL aceitar somente quantidade inteira positiva, preço de execução positivo com até quatro casas decimais e data que não esteja no futuro.

#### Scenario: Preço de execução inválido

- WHEN uma compra ou venda é solicitada com preço zero, negativo ou não numérico
- THEN a operação é rejeitada
- AND a posição e o histórico não são alterados

#### Scenario: Quantidade fracionária

- WHEN uma compra ou venda é solicitada com quantidade fracionária
- THEN a operação é rejeitada
- AND a posição e o histórico não são alterados

#### Scenario: Data futura

- WHEN uma negociação é enviada com data futura
- THEN a operação é rejeitada

### Requirement: Consistência concorrente

O sistema SHALL bloquear a posição durante o cálculo e a persistência da negociação.

#### Scenario: Duas vendas simultâneas

- GIVEN duas vendas concorrentes para o mesmo ativo
- WHEN a primeira obtém o lock da posição
- THEN a segunda aguarda o saldo atualizado
- AND não utiliza um saldo anterior para autorizar a venda

### Requirement: Histórico consultável

O sistema SHALL listar negociações da data mais recente para a mais antiga com todos os valores financeiros relevantes.

#### Scenario: Consulta do histórico

- WHEN o usuário abre a tela ou consulta `GET /negociacoes`
- THEN vê ativo, tipo, quantidade, preço, bruto, taxas, total, resultado realizado e data

### Requirement: Interface de negociação

O sistema SHALL oferecer uma tela autenticada para comprar e vender ativos cadastrados, permitindo alterar o preço de execução e sem permitir alterar a taxa.

#### Scenario: Operação concluída pela interface

- GIVEN um usuário autenticado e um ativo cadastrado
- WHEN ele preenche dados válidos e confirma
- THEN a cotação atual é sugerida no campo de preço
- AND o usuário pode trocar o preço antes de confirmar
- AND visualiza previamente preço informado, bruto, total e saldo projetado
- THEN recebe uma mensagem de sucesso
- AND a nova operação aparece no histórico
- AND o dashboard reflete a posição recalculada

#### Scenario: Campos financeiros estritamente numéricos

- WHEN o usuário tenta digitar letras nos campos de quantidade ou preço
- THEN os caracteres não numéricos são removidos
- AND a quantidade aceita apenas números inteiros positivos
- AND o preço aceita no máximo quatro dígitos decimais
