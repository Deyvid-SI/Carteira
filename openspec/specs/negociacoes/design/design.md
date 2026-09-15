# Design: compras e vendas de ações

## Modelo

`Negociacao` pertence a `Acao` e armazena tipo, quantidade, preço executado, taxa, valores bruto/líquido, resultado realizado e datas. A interface sugere a cotação atual, mas o preço executado é informado pelo usuário para representar o valor real da operação. A taxa de novas operações é definida pelo backend como zero e não é aceita como escolha do usuário.

## Compra

```text
preçoExecutado = preço informado na compra
bruto = quantidade × preçoExecutado
taxa = 0
desembolso = bruto
novoSaldo = saldo + quantidade
novoPM = (saldo × PM atual + desembolso) ÷ novoSaldo
```

O custo da posição usa o valor bruto. Compra não gera resultado realizado.

## Venda

```text
preçoExecutado = preço informado na venda
bruto = quantidade × preçoExecutado
taxa = 0
recebido = bruto
custoVendido = quantidade × PM atual
resultadoRealizado = recebido − custoVendido
novoSaldo = saldo − quantidade
```

O preço executado da venda nunca participa do cálculo do preço médio. Em uma venda parcial, o preço médio restante é preservado exatamente, independentemente de haver lucro ou prejuízo. Liquidação total zera saldo e preço médio para permitir que uma futura compra inicie uma nova posição.

## Consistência

`NegociacaoService.registrar` usa `@Transactional`. A ação é carregada com `PESSIMISTIC_WRITE`, impedindo duas vendas simultâneas de autorizarem o mesmo saldo antigo.

## Validações

- Ativo e tipo obrigatórios.
- Quantidade inteira positiva e preço de execução positivo, com até quatro casas decimais.
- Data não futura.
- Venda limitada ao saldo.

## Consulta

Repository usa `EntityGraph` para carregar o ativo junto da negociação e evitar acesso lazy fora da transação. Histórico é ordenado por data e id descendentes.

## Interface e API

`/gestao/negociacoes` oferece formulário, cotação atual como sugestão, preço editável, taxa somente leitura, prévia dos valores e tabela. A quantidade usa teclado inteiro; o preço usa teclado decimal; ambos removem letras. `POST /negociacoes` registra; `GET /negociacoes` lista com paginação. Dashboard e tabela de ativos direcionam ao fluxo.

## Testes

São verificados preço informado, taxa automática zero, preço médio, venda parcial, resultado realizado, excesso de saldo, preço inválido, migration, endpoints e renderização.
