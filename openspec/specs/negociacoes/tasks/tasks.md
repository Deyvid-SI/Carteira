# Tasks: compras e vendas de ações

## Domínio e banco

- [x] Criar enum de compra e venda.
- [x] Criar entidade e repository de negociações.
- [x] Relacionar negociação ao ativo.
- [x] Criar migration com FK e índices.
- [x] Manter migrations anteriores imutáveis.

## Regras financeiras

- [x] Calcular bruto e total com taxa automática zero.
- [x] Recalcular preço médio nas compras.
- [x] Reduzir saldo e manter PM nas vendas.
- [x] Calcular resultado realizado.
- [x] Zerar posição na liquidação total.
- [x] Bloquear excesso de saldo, data futura e valores inválidos.
- [x] Adicionar lock pessimista por ativo.
- [x] Usar a cotação atual como preço de execução.
- [x] Rejeitar ativo sem cotação válida.

## API e interface

- [x] Criar DTOs validados.
- [x] Criar endpoints POST e GET paginado.
- [x] Criar tela de compra e venda.
- [x] Exibir histórico financeiro completo.
- [x] Adicionar item ao menu e atalhos no dashboard/ativos.
- [x] Retirar edição direta da tabela visual.
- [x] Retirar entrada manual de preço da negociação.
- [x] Exibir prévia de bruto, total e saldo após a operação.
- [x] Remover entrada manual de taxa da interface e da API.
- [x] Bloquear letras e limitar quatro casas no campo de quantidade.

## Verificação

- [x] Testar compra, preço médio e taxa automática zero.
- [x] Testar venda e resultado realizado.
- [x] Testar rejeição sem persistência parcial.
- [x] Testar preço automático e cotação inválida.
- [x] Aplicar migration no banco existente.
- [x] Validar tela, API e OpenSpec.
