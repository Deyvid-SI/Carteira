# Proposta: compras e vendas de ações

## Why

Editar quantidade e preço médio diretamente não explica a formação da posição, não registra movimentações e não calcula resultado realizado. Compras e vendas precisam formar um histórico financeiro consistente.

## What

- Registrar compra ou venda ligada a um ativo.
- Usar a cotação atual persistida como preço de execução, sem permitir preço arbitrário na interface.
- Definir taxa automática zero, sem entrada manual na interface ou API.
- Persistir quantidade, preço executado, taxa calculada, bruto, total e resultado realizado.
- Recalcular saldo e preço médio automaticamente.
- Bloquear venda acima do saldo e dados financeiros inválidos.
- Oferecer tela com prévia financeira e campos estritamente numéricos, API e histórico cronológico.
- Proteger concorrência sobre a mesma posição.

## Impact

Entidade, enum, DTOs, repository, service, controllers, template, menu, migration Liquibase e testes financeiros.

## Fora de escopo

Ordens reais em corretora, tributação, FIFO/LIFO, notas de corretagem, proventos e conversão cambial.

## Critério de conclusão

Posição e negociação devem ser gravadas atomicamente; uma falha nunca pode deixar saldo alterado sem histórico correspondente.
