# Dashboard Specification

## Purpose

Apresentar uma visão consolidada e legível dos dados persistidos.

## Requirements

### Requirement: Indicadores e tabelas

O sistema SHALL exibir totais de ativos, corretoras validadas e ativos por mercado, além de tabela com preço, variação e atualização.

#### Scenario: Carteira sem dados

- GIVEN que nenhum ativo foi cadastrado
- WHEN o dashboard é aberto
- THEN os totais são zero
- AND a tabela e o gráfico exibem estados vazios explicativos

### Requirement: Gráficos sem dependência externa de interface

O sistema SHALL desenhar no navegador, usando `canvas` local, gráficos de cotações, desempenho normalizado, preço médio acumulado, mercados, alocação por moeda, rentabilidade, variação diária e capital investido versus atual.

#### Scenario: Dados dos dois mercados

- GIVEN ativos brasileiros e americanos cadastrados
- WHEN o dashboard carrega `/dashboard/dados`
- THEN o gráfico de distribuição contém os dois mercados
- AND o gráfico histórico apresenta até oito séries
- AND valores de BRL e USD são consolidados separadamente

### Requirement: Preço médio acumulado por ação

O sistema SHALL apresentar o preço médio acumulado atual de cada ação em colunas.

#### Scenario: Compra altera o preço médio

- GIVEN uma posição inicial de 10 unidades com preço médio 100
- AND uma compra de 5 unidades pela cotação 120
- WHEN o dashboard é carregado
- THEN a coluna do ativo apresenta o preço médio 106,6667
- AND cada ação possui sua própria coluna
- AND o gráfico identifica a moeda selecionada
- AND BRL e USD podem ser visualizados separadamente

### Requirement: Resumo financeiro completo

O sistema SHALL exibir para BRL e USD o total investido, patrimônio atual, resultado acumulado, rentabilidade e resultado estimado do dia.

#### Scenario: Carteira multimoeda

- GIVEN posições em BRL e USD
- WHEN o dashboard é aberto
- THEN cada moeda possui seu próprio resumo e gráfico de alocação
- AND o sistema não soma numericamente reais e dólares

### Requirement: Interface responsiva

O sistema SHALL manter navegação e tabelas utilizáveis em desktop e dispositivos móveis.

#### Scenario: Tela estreita

- WHEN a largura da viewport é inferior a 820 pixels
- THEN a navegação passa ao topo
- AND painéis são organizados em uma coluna
