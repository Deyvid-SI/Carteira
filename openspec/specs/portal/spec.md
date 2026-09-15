# Portal Specification

## Purpose

Complementar a gestão da carteira com preferências de interface, histórico completo e organização anual para apoio ao Imposto de Renda.

## Requirements

### Requirement: Histórico completo

O sistema SHALL apresentar todas as compras e vendas persistidas da mais recente para a mais antiga, com totais separados por moeda.

#### Scenario: Filtrar operações

- GIVEN que existem compras e vendas nos mercados brasileiro e americano
- WHEN o usuário informa ticker, tipo ou mercado nos filtros
- THEN somente as linhas correspondentes permanecem visíveis
- AND o contador é atualizado sem nova requisição ao servidor

### Requirement: Apoio ao Imposto de Renda

O sistema SHALL consolidar, por ano, o valor bruto das vendas e o resultado realizado, mantendo BRL e USD separados.

#### Scenario: Ano com compra e venda

- GIVEN uma compra e uma venda no mesmo ano
- WHEN o resumo fiscal é aberto
- THEN ambas contam como operações
- AND apenas o valor bruto da venda compõe o total de alienações
- AND o resultado realizado da venda é exibido sem ser tratado como imposto devido

#### Scenario: Limitação fiscal

- WHEN a tela de Imposto de Renda é exibida
- THEN há um aviso visível de que ela não substitui informes, contador ou declaração oficial
- AND a tela solicita conferência de day trade, câmbio, eventos corporativos e prejuízos anteriores

### Requirement: Preferências locais

O sistema SHALL permitir escolher tema, nível de movimento e densidade, persistindo as opções apenas no navegador.

#### Scenario: Movimento reduzido

- WHEN o usuário escolhe movimento reduzido
- THEN o documento recebe `data-motion="reduced"`
- AND animações decorativas são interrompidas

### Requirement: Segurança das novas telas

O sistema SHALL exigir autenticação para configurações, histórico e Imposto de Renda.

#### Scenario: Visitante não autenticado

- WHEN um visitante abre qualquer uma das três rotas
- THEN ele é redirecionado para o login
