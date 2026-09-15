# Proposta: compras e vendas de ações

## Why

Editar quantidade e preço médio diretamente não explica a formação da posição, não registra movimentações e não calcula resultado realizado. Compras e vendas precisam formar um histórico financeiro consistente.

## What

- Registrar compra ou venda ligada a um ativo.
- Sugerir a cotação atual, permitindo informar o preço real executado na compra ou venda.
- Definir taxa automática zero, sem entrada manual na interface ou API.
- Persistir quantidade, preço executado, taxa calculada, bruto, total e resultado realizado.
- Recalcular o preço médio somente nas compras e preservá-lo nas vendas parciais.
- Bloquear venda acima do saldo e dados financeiros inválidos.
- Oferecer tela com prévia financeira e campos estritamente numéricos, API e histórico cronológico.
- Proteger concorrência sobre a mesma posição.

## Impact

Entidade, enum, DTOs, repository, service, controllers, template, menu, migration Liquibase e testes financeiros.

## Fora de escopo

Ordens reais em corretora, tributação, FIFO/LIFO, notas de corretagem, proventos e conversão cambial.

## Critério de conclusão

Posição e negociação devem ser gravadas atomicamente; uma falha nunca pode deixar saldo alterado sem histórico correspondente.
