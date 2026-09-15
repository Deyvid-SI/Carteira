# Proposta: dashboard financeiro

## Why

Dados isolados em tabelas não permitem avaliar rapidamente patrimônio, alocação, rentabilidade e comportamento histórico. O usuário precisa de uma visão consolidada sem misturar moedas diferentes.

## What

- Exibir indicadores cadastrais e financeiros.
- Consolidar BRL e USD separadamente.
- Mostrar todas as posições com quantidade, custo, cotação e resultados.
- Desenhar doze gráficos locais e responsivos.
- Exibir todos os ativos brasileiros e americanos nas visualizações de preço médio, com filtro por mercado e rolagem quando necessário.
- Exibir estados vazios quando não houver dados.

## Gráficos

1. evolução de cotações;
2. quantidade por mercado;
3. desempenho histórico normalizado;
4. preço médio atual por ação;
5. preço médio versus cotação atual;
6. distância percentual da cotação para o preço médio;
7. evolução do preço médio do ativo selecionado;
8. rentabilidade por ativo;
9. alocação BRL;
10. alocação USD;
11. variação diária;
12. investido versus atual.

## Impact

`DashboardService`, DTO consolidado, endpoint `/dashboard/dados`, template, JavaScript Canvas e CSS responsivo.

## Critério de conclusão

Todos os valores persistidos devem aparecer de modo coerente, sem somar BRL com USD e sem depender de biblioteca visual externa.
