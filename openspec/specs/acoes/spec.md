# Ações Specification

## Purpose

Gerenciar ativos brasileiros e americanos a partir de cotações externas verificáveis.

## Requirements

### Requirement: Provedor por mercado

O sistema SHALL direcionar tickers brasileiros à brapi e tickers americanos à Alpha Vantage.

#### Scenario: Ação brasileira válida

- GIVEN o mercado `BRASIL` e um ticker coberto pela brapi
- WHEN o cadastro é solicitado
- THEN nome, moeda, preço, variação, horário e histórico disponíveis são persistidos

#### Scenario: Ação americana válida

- GIVEN o mercado `ESTADOS_UNIDOS`, uma chave configurada e um ticker coberto pela Alpha Vantage
- WHEN o cadastro é solicitado
- THEN a cotação mais recente em USD é persistida

#### Scenario: Ticker inexistente

- GIVEN um ticker que o provedor não reconhece
- WHEN o cadastro é solicitado
- THEN nenhuma ação é persistida
- AND a falha externa é apresentada de forma controlada

### Requirement: Ticker único por carteira

O sistema SHALL comparar tickers sem diferenciar maiúsculas e minúsculas e impedir duplicidade dentro da mesma carteira.

#### Scenario: Repetição com caixa diferente

- GIVEN que `PETR4` está cadastrada
- WHEN o usuário tenta cadastrar `petr4`
- THEN a operação é rejeitada

#### Scenario: Mesmo ticker em contas diferentes

- GIVEN que `PETR4` pertence à carteira de uma conta
- WHEN outra conta cadastra `PETR4`
- THEN o cadastro é permitido
- AND cada conta visualiza somente a própria posição

### Requirement: Histórico de cotações

O sistema SHALL registrar pontos históricos importados no cadastro e um novo ponto a cada atualização.

#### Scenario: Atualização pelo provedor

- GIVEN uma ação cadastrada
- WHEN a atualização é solicitada
- THEN a cotação atual é substituída pela resposta do provedor
- AND um ponto é acrescentado ao histórico

### Requirement: Posição financeira

O sistema SHALL registrar a quantidade de cada ativo em unidades inteiras, manter seu preço médio e calcular valor investido, valor atual, resultado, rentabilidade e resultado diário estimado.

#### Scenario: Posição com lucro

- GIVEN 10 unidades com preço médio 20 e cotação atual 25
- WHEN os valores da posição são consultados
- THEN o valor investido é 200
- AND o valor atual é 250
- AND o resultado é 50, equivalente a 25%

#### Scenario: Atualização da posição

- GIVEN um ativo cadastrado
- WHEN quantidade ou preço médio são alterados
- THEN todos os valores financeiros e gráficos são recalculados sem consultar novamente a cotação

#### Scenario: Saldo fracionário

- WHEN o usuário tenta cadastrar, editar, comprar ou vender uma quantidade com casas decimais
- THEN a operação é rejeitada
- AND o saldo da posição permanece inalterado

### Requirement: Identificação visual da empresa

O sistema SHALL associar ao ativo uma URL de logo derivada do ticker e exibir uma inicial legível quando a imagem não puder ser carregada.

#### Scenario: Logos dos dois mercados

- GIVEN `PETR4` do mercado brasileiro e `AAPL` do mercado americano
- WHEN os ativos são exibidos
- THEN a ação brasileira usa o símbolo `PETR4.SA` para localizar a logo
- AND a ação americana usa o símbolo `AAPL`
- AND uma falha de imagem revela a inicial do ticker sem quebrar a tabela
