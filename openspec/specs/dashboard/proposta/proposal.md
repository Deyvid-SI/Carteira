# Proposta: dashboard financeiro

## Why

Dados isolados em tabelas não permitem avaliar rapidamente patrimônio, alocação, rentabilidade e comportamento histórico. O usuário precisa de uma visão consolidada sem misturar moedas diferentes.

## What

- Exibir indicadores cadastrais e financeiros.
- Consolidar BRL e USD separadamente.
- Mostrar todas as posições com quantidade, custo, cotação e resultados.
- Desenhar nove gráficos locais e responsivos.
- Exibir estados vazios quando não houver dados.

## Gráficos

1. evolução de cotações;
2. quantidade por mercado;
3. desempenho histórico normalizado;
4. preço médio acumulado por ação;
5. rentabilidade por ativo;
6. alocação BRL;
7. alocação USD;
8. variação diária;
9. investido versus atual.

## Impact

`DashboardService`, DTO consolidado, endpoint `/dashboard/dados`, template, JavaScript Canvas e CSS responsivo.

## Critério de conclusão

Todos os valores persistidos devem aparecer de modo coerente, sem somar BRL com USD e sem depender de biblioteca visual externa.
