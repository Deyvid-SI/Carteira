# Delta: negociações de ações

## ADDED Requirements

### Requirement: Registro de compra

O sistema SHALL registrar uma compra e atualizar quantidade e preço médio do ativo na mesma transação.

#### Scenario: Compra com taxas

- GIVEN 10 unidades com preço médio 100
- WHEN são compradas 5 unidades a 130 com taxas de 5
- THEN o saldo passa a 15 unidades
- AND o desembolso registrado é 655
- AND o novo preço médio é 110,3333
- AND o resultado realizado da compra é zero

### Requirement: Registro de venda

O sistema SHALL registrar uma venda, reduzir o saldo e calcular o resultado realizado líquido de taxas.

#### Scenario: Venda parcial com lucro

- GIVEN 10 unidades com preço médio 100
- WHEN são vendidas 4 unidades a 150 com taxas de 10
- THEN o saldo passa a 6 unidades
- AND o preço médio permanece 100
- AND o valor recebido é 590
- AND o resultado realizado é 190

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

O sistema SHALL aceitar somente quantidade e preço positivos, taxas não negativas e data que não esteja no futuro.

#### Scenario: Data futura

- WHEN uma negociação é enviada com data futura
- THEN a operação é rejeitada

#### Scenario: Taxa de venda superior ao bruto

- WHEN as taxas de uma venda superam quantidade multiplicada pelo preço
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

O sistema SHALL oferecer uma tela autenticada para comprar e vender ativos cadastrados.

#### Scenario: Operação concluída pela interface

- GIVEN um usuário autenticado e um ativo cadastrado
- WHEN ele preenche dados válidos e confirma
- THEN recebe uma mensagem de sucesso
- AND a nova operação aparece no histórico
- AND o dashboard reflete a posição recalculada
