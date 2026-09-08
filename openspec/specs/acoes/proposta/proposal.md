# Proposta: gestão de ações

## Why

A carteira precisa validar ativos em fontes reais antes de persistir dados, separar mercados brasileiro e americano e manter cotações suficientes para cálculos e gráficos.

## What

- Cadastrar tickers brasileiros pela brapi e americanos pela Alpha Vantage.
- Impedir ticker duplicado sem diferenciar maiúsculas e minúsculas.
- Persistir cotação, moeda, variação, data e histórico.
- Manter quantidade, preço médio e indicadores derivados da posição.
- Permitir atualização manual da cotação sem alterar o custo da posição.

## Scope

Inclui cadastro, consulta, atualização de cotação, posição consolidada e histórico. Compra e venda são detalhadas na spec `negociacoes`.

## Impact

- Entidades `Acao` e `HistoricoCotacao`.
- Gateways de cotação e seleção por mercado.
- Controllers REST/web, serviços e repositories.
- Dashboard e tabela de ativos dependem desses dados.

## Riscos

- Limites ou indisponibilidade das APIs externas.
- Cotação de fechamento em vez de tempo real.
- Diferenças de moeda e calendário entre mercados.

## Critério de conclusão

Um ticker válido deve ser persistido com dados verificáveis; ticker inválido ou duplicado não deve alterar o banco.
