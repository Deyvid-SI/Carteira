# Design: gestão de ações

## Arquitetura

```text
Controller → AcaoService → CotacaoGatewaySelector → brapi/Alpha Vantage
                         → AcaoRepository
                         → HistoricoCotacaoRepository
```

`CotacaoGateway` funciona como porta interna. Cada cliente converte o payload externo para `CotacaoDados`, evitando que formatos de terceiros vazem para o domínio.

## Seleção do provedor

- `Mercado.BRASIL` usa brapi, moeda BRL e bolsa B3.
- `Mercado.ESTADOS_UNIDOS` usa Alpha Vantage, moeda USD e NYSE/Nasdaq.

## Persistência

`Acao` guarda o estado atual e pertence obrigatoriamente a um usuário. `HistoricoCotacao` guarda pontos temporais ligados por chave estrangeira. O ticker possui constraint única por usuário e é normalizado em maiúsculas.

A quantidade é persistida em `DECIMAL(19,0)` e validada como unidade inteira não negativa. Valores fracionários são rejeitados antes de qualquer alteração da carteira.

`Acao.getLogoUrl()` deriva uma imagem pública pelo ticker. Ativos da B3 recebem o sufixo `.SA`; ativos americanos usam o ticker original. A interface carrega a imagem de forma preguiçosa e troca para uma inicial local se o recurso externo falhar.

## Cálculos

```text
valorInvestido = quantidade × preçoMédio
valorAtual = quantidade × cotaçãoAtual
resultado = valorAtual − valorInvestido
rentabilidade = resultado ÷ valorInvestido × 100
```

Resultado diário é estimado a partir da variação percentual informada pelo provedor.

## Fluxo de cadastro

1. Validar formato e mercado.
2. Normalizar ticker.
3. Rejeitar duplicidade antes da chamada externa.
4. Consultar o gateway correto.
5. Persistir ação e histórico na mesma transação.

## Falhas

- Recurso inexistente: HTTP 404.
- Ticker/regra inválida: HTTP 422.
- Provedor indisponível ou limitado: HTTP 502.

## Verificação

Testes cobrem duplicidade, seleção de gateway, cálculos financeiros e persistência aceita pelo schema Liquibase.
