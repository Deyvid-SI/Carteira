# Design: dashboard financeiro

## Fluxo

```text
dashboard.html → dashboard.js → GET /dashboard/dados → DashboardService → repositories
```

O backend prepara séries e agregados; o navegador apenas formata e desenha. Isso mantém regras financeiras fora do JavaScript.

## Consolidação monetária

Cada ativo conserva sua moeda original. Resumos são agrupados em `ResumoMoeda`, impedindo soma direta entre reais e dólares.

## Séries históricas

Até oito ativos e trinta pontos por série são enviados. Desempenho normalizado usa o primeiro ponto como base zero:

```text
desempenho% = (valorAtual ÷ primeiroValor − 1) × 100
```

O preço médio acumulado é reconstruído pelo histórico de negociações. Compras adicionam o total desembolsado ao custo da posição; vendas retiram o custo contábil vendido sem alterar o preço médio das unidades restantes. A posição anterior à primeira negociação é reconstruída a partir do saldo atual para também representar posições iniciais cadastradas manualmente.

Na interface, o último valor acumulado de cada ação é desenhado como uma coluna. As ações são filtradas por moeda, evitando comparar diretamente reais e dólares no mesmo eixo.

## Renderização

Canvas local elimina CDN e funciona sem baixar bibliotecas adicionais. Funções compartilhadas tratam eixos, escalas, cores, legendas, redimensionamento e valores positivos/negativos.

## Responsividade

- Desktop: grades com múltiplas colunas.
- Até 1100 px: gráficos principais em uma coluna.
- Até 820 px: menu horizontal no topo.
- Até 560 px: cartões e campos em coluna única.

## Estados vazios

Canvas e tabela mostram mensagens explicativas em vez de erro ou área em branco.

## Testes

Service é testado com múltiplas moedas, séries e preço médio acumulado. Endpoints e página protegida são validados por MockMvc e execução local.
