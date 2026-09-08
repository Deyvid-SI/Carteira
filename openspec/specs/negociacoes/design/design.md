# Design: compras e vendas de ações

## Modelo

`Negociacao` pertence a `Acao` e armazena tipo, quantidade, preço executado, taxa, valores bruto/líquido, resultado realizado e datas. O preço executado é copiado da cotação atual persistida e a taxa de novas operações é definida pelo backend como zero; nenhum dos dois é aceito como escolha do usuário.

## Compra

```text
preçoExecutado = cotaçãoAtual
bruto = quantidade × preçoExecutado
taxa = 0
desembolso = bruto
novoSaldo = saldo + quantidade
novoPM = (saldo × PM atual + desembolso) ÷ novoSaldo
```

O custo da posição usa o valor bruto. Compra não gera resultado realizado.

## Venda

```text
preçoExecutado = cotaçãoAtual
bruto = quantidade × preçoExecutado
taxa = 0
recebido = bruto
custoVendido = quantidade × PM atual
resultadoRealizado = recebido − custoVendido
novoSaldo = saldo − quantidade
```

O preço médio restante não muda. Liquidação total zera saldo e preço médio.

## Consistência

`NegociacaoService.registrar` usa `@Transactional`. A ação é carregada com `PESSIMISTIC_WRITE`, impedindo duas vendas simultâneas de autorizarem o mesmo saldo antigo.

## Validações

- Ativo e tipo obrigatórios.
- Quantidade e cotação atual positivas.
- Data não futura.
- Venda limitada ao saldo.

## Consulta

Repository usa `EntityGraph` para carregar o ativo junto da negociação e evitar acesso lazy fora da transação. Histórico é ordenado por data e id descendentes.

## Interface e API

`/gestao/negociacoes` oferece formulário, cotação e taxa somente leitura, prévia dos valores e tabela. O campo de quantidade usa teclado decimal e sanitização que remove letras. `POST /negociacoes` registra; `GET /negociacoes` lista com paginação. Dashboard e tabela de ativos direcionam ao fluxo.

## Testes

São verificados uso obrigatório da cotação atual, taxa automática zero, preço médio, venda parcial, resultado realizado, excesso de saldo, cotação inválida, migration, endpoints e renderização.
