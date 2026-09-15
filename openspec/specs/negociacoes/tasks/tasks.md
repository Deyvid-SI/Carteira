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
- [x] Reduzir saldo e preservar exatamente o PM nas vendas, independentemente do preço executado.
- [x] Calcular resultado realizado.
- [x] Zerar posição na liquidação total.
- [x] Bloquear excesso de saldo, data futura e valores inválidos.
- [x] Aceitar apenas quantidades inteiras na compra e na venda.
- [x] Adicionar lock pessimista por ativo.
- [x] Sugerir a cotação atual e usar o preço informado como preço de execução.
- [x] Rejeitar preço de execução ausente, zero ou negativo.

## API e interface

- [x] Criar DTOs validados.
- [x] Criar endpoints POST e GET paginado.
- [x] Criar tela de compra e venda.
- [x] Exibir histórico financeiro completo.
- [x] Adicionar item ao menu e atalhos no dashboard/ativos.
- [x] Retirar edição direta da tabela visual.
- [x] Permitir entrada manual de preço na compra e na venda.
- [x] Exibir prévia de bruto, total e saldo após a operação.
- [x] Remover entrada manual de taxa da interface e da API.
- [x] Bloquear letras, exigir quantidade inteira e limitar o preço a quatro casas decimais.

## Verificação

- [x] Testar compra, preço médio e taxa automática zero.
- [x] Testar vendas com lucro e prejuízo, garantindo PM inalterado no serviço e no gráfico.
- [x] Testar rejeição sem persistência parcial.
- [x] Testar preço informado e rejeição de preço inválido.
- [x] Aplicar migration no banco existente.
- [x] Validar tela, API e OpenSpec.
