# Design: dashboard financeiro

## Fluxo

```text
dashboard.html → dashboard.js → GET /dashboard/dados → DashboardService → repositories
```

O backend prepara séries e agregados; o navegador apenas formata e desenha. Isso mantém regras financeiras fora do JavaScript.

## Consolidação monetária

Cada ativo conserva sua moeda original. Resumos são agrupados em `ResumoMoeda`, impedindo soma direta entre reais e dólares.

## Séries históricas

Os gráficos gerais de histórico enviam até oito ativos e trinta pontos por série. A central de preço médio recebe todos os ativos cadastrados, sem `limit`, e usa rolagem horizontal para preservar a leitura de cada coluna. Desempenho normalizado usa o primeiro ponto como base zero:

```text
desempenho% = (valorAtual ÷ primeiroValor − 1) × 100
```

O preço médio acumulado é reconstruído pelo histórico de negociações. Compras adicionam o total desembolsado ao custo da posição; vendas retiram somente o custo contábil pelo preço médio vigente, nunca pelo preço de venda. Assim, lucro ou prejuízo realizado não altera o preço médio das unidades restantes. A posição anterior à primeira negociação é reconstruída a partir do saldo atual para também representar posições iniciais cadastradas manualmente.

Na interface, uma central reúne quatro leituras complementares: preço médio atual por ação, preço médio versus cotação, distância percentual acima/abaixo do custo e evolução histórica do ativo selecionado. Um único filtro alterna Brasil/BRL e Estados Unidos/USD, evitando comparar diretamente reais e dólares no mesmo eixo.

## Renderização

Canvas local elimina CDN e funciona sem baixar bibliotecas adicionais. Funções compartilhadas tratam eixos, escalas, cores, legendas, redimensionamento e valores positivos/negativos.

## Responsividade

- Desktop: barra superior rolável com dez abas e grades com múltiplas colunas.
- Até 1100 px: gráficos principais em uma coluna e dados compactos da sessão.
- Até 820 px: abas em uma segunda linha horizontal rolável.
- Até 560 px: cartões e campos em coluna única.

## Aparência e movimento

`theme.js` aplica `data-theme`, `data-motion` e `data-density` no elemento raiz antes da folha de estilos e persiste as escolhas em `localStorage`. O evento `carteira:themechange` faz o dashboard recalcular a paleta dos canvas. O tema escuro usa preto piano, dourado e uma textura original de mármore; o claro usa branco, marfim e dourado. O deslocamento do mármore, os halos e o brilho da navegação são interrompidos por `prefers-reduced-motion` ou pela preferência explícita de movimento reduzido.

As abas de resumo, preço médio, resultados e posições usam âncoras. Um `IntersectionObserver` destaca a seção atualmente visível sem chamadas ao backend.

## Estados vazios

Canvas e tabela mostram mensagens explicativas em vez de erro ou área em branco.

## Testes

Service é testado com múltiplas moedas, séries, preço médio acumulado e mais de oito ativos nos dois mercados. Endpoints e página protegida são validados por MockMvc e execução local.
