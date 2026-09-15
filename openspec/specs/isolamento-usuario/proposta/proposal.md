# Proposta: isolamento das carteiras

## Problema

Os registros financeiros eram consultados de forma global, fazendo uma conta nova visualizar posições e movimentações já cadastradas por outras pessoas.

## Mudança

Adicionar propriedade por usuário às ações e corretoras e aplicar esse limite a todas as consultas e operações financeiras. Negociações, históricos de cotação, dashboard e relatórios herdam o proprietário da ação.

## Escopo

- Relacionar ações e corretoras à conta autenticada.
- Filtrar leituras, alterações e cadastros pela conta.
- Tornar ticker e CNPJ únicos somente dentro da carteira.
- Migrar registros existentes para a conta mais antiga, preservando o ambiente já utilizado.

## Critério de conclusão

Uma conta nova inicia vazia, não consegue consultar identificadores alheios e pode manter os mesmos tickers de outra conta sem compartilhar saldos.
