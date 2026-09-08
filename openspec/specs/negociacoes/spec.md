# Negociações Specification

## Purpose

Registrar compras e vendas com consistência financeira, histórico auditável e atualização automática das posições.

## Requirements

### Requirement: Registro de compra

O sistema SHALL registrar uma compra e atualizar quantidade e preço médio do ativo na mesma transação.

#### Scenario: Compra pela cotação atual e sem taxa manual

- GIVEN 10 unidades com preço médio 100 e cotação atual 120
- WHEN são compradas 5 unidades
- THEN o saldo passa a 15 unidades
- AND o preço executado automaticamente é 120
- AND a taxa registrada automaticamente é zero
- AND o desembolso registrado é 600
- AND o novo preço médio é 106,6667
- AND o resultado realizado da compra é zero

### Requirement: Registro de venda

O sistema SHALL registrar uma venda, reduzir o saldo e calcular o resultado realizado com taxa automática zero.

#### Scenario: Venda parcial com lucro

- GIVEN 10 unidades com preço médio 100 e cotação atual 120
- WHEN são vendidas 4 unidades
- THEN o saldo passa a 6 unidades
- AND o preço médio permanece 100
- AND o preço executado automaticamente é 120
- AND a taxa registrada automaticamente é zero
- AND o valor recebido é 480
- AND o resultado realizado é 80

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

O sistema SHALL aceitar somente quantidade positiva, cotação atual válida e data que não esteja no futuro.

#### Scenario: Cotação indisponível

- GIVEN um ativo sem cotação positiva
- WHEN uma compra ou venda é solicitada
- THEN a operação é rejeitada
- AND o usuário é orientado a atualizar a cotação

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

O sistema SHALL oferecer uma tela autenticada para comprar e vender ativos cadastrados, sem permitir que o usuário altere preço ou taxa da execução.

#### Scenario: Operação concluída pela interface

- GIVEN um usuário autenticado e um ativo cadastrado
- WHEN ele preenche dados válidos e confirma
- THEN visualiza previamente preço atual, bruto, total e saldo projetado
- THEN recebe uma mensagem de sucesso
- AND a nova operação aparece no histórico
- AND o dashboard reflete a posição recalculada

#### Scenario: Campo de quantidade estritamente numérico

- WHEN o usuário tenta digitar letras no campo de quantidade
- THEN os caracteres não numéricos são removidos
- AND são aceitos no máximo quatro dígitos decimais
