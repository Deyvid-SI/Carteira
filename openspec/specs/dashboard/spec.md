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

O sistema SHALL desenhar no navegador, usando `canvas` local, gráficos de cotações, desempenho normalizado, quatro visualizações de preço médio, mercados, alocação por moeda, rentabilidade, variação diária e capital investido versus atual.

#### Scenario: Dados dos dois mercados

- GIVEN ativos brasileiros e americanos cadastrados
- WHEN o dashboard carrega `/dashboard/dados`
- THEN o gráfico de distribuição contém os dois mercados
- AND o gráfico histórico apresenta até oito séries
- AND valores de BRL e USD são consolidados separadamente

### Requirement: Preço médio acumulado por ação

O sistema SHALL apresentar, sem limitar a quantidade de ativos, o preço médio atual em colunas, sua comparação com a cotação, a distância percentual e a evolução histórica selecionável.

#### Scenario: Compra altera o preço médio

- GIVEN uma posição inicial de 10 unidades com preço médio 100
- AND uma compra de 5 unidades com preço informado de 120
- WHEN o dashboard é carregado
- THEN a coluna do ativo apresenta o preço médio 106,6667

#### Scenario: Venda não altera o preço médio

- GIVEN uma posição com preço médio 100
- WHEN parte das unidades é vendida por 130 ou por 70
- THEN a coluna do ativo continua apresentando o preço médio 100

#### Scenario: Todos os ativos dos dois mercados

- GIVEN mais de oito ações brasileiras e americanas cadastradas
- WHEN a central de preço médio é aberta
- THEN cada ação do mercado selecionado possui sua própria coluna
- AND nenhum ativo é removido por limite de quantidade
- AND Brasil/BRL e Estados Unidos/USD podem ser visualizados separadamente
- AND a área permite rolagem horizontal quando todas as colunas não couberem na tela
- AND uma faixa identifica cada ação com logo, ticker e nome da empresa

### Requirement: Resumo financeiro completo

O sistema SHALL exibir para BRL e USD o total investido, patrimônio atual, resultado acumulado, rentabilidade e resultado estimado do dia.

#### Scenario: Carteira multimoeda

- GIVEN posições em BRL e USD
- WHEN o dashboard é aberto
- THEN cada moeda possui seu próprio resumo e gráfico de alocação
- AND o sistema não soma numericamente reais e dólares

### Requirement: Interface responsiva

O sistema SHALL manter navegação superior por abas e tabelas utilizáveis em desktop e dispositivos móveis.

#### Scenario: Tela estreita

- WHEN a largura da viewport é inferior a 820 pixels
- THEN as abas superiores permitem rolagem horizontal
- AND painéis são organizados em uma coluna

### Requirement: Temas dourados

O sistema SHALL permitir alternância entre os temas preto piano com dourado e branco com dourado, persistindo a preferência no navegador e redesenhando os gráficos com contraste adequado.

#### Scenario: Alternância para modo claro

- GIVEN que o tema preto piano está ativo
- WHEN o usuário aciona `Modo claro`
- THEN superfícies e fundos passam para branco e dourado
- AND a preferência é salva no armazenamento local
- AND os gráficos são redesenhados com textos e linhas legíveis

#### Scenario: Movimento reduzido

- GIVEN que o sistema operacional solicita redução de movimento
- WHEN a interface é carregada
- THEN animações de fundo e transições são reduzidas ao mínimo

### Requirement: Abas superiores

O sistema SHALL exibir dez abas superiores para resumo, preço médio, resultados, posições, ativos, negociação, histórico, corretoras, Imposto de Renda e configurações.

#### Scenario: Navegação dentro do dashboard

- WHEN o usuário percorre as seções do dashboard
- THEN a aba da seção visível recebe destaque
- AND os atalhos usam âncoras com deslocamento para a barra superior fixa

### Requirement: Identidade visual financeira

O sistema SHALL usar uma marca geométrica própria e uma textura original de mármore preto com veios dourados no tema escuro.

#### Scenario: Fundo em movimento

- GIVEN que o tema escuro e o movimento suave estão ativos
- WHEN uma tela autenticada é aberta
- THEN a textura de mármore aparece atrás dos painéis com deslocamento lento
- AND nenhum texto, logotipo ou marca-d'água faz parte do arquivo de fundo
