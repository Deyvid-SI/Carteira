# Tasks: gestão de ações

## Domínio

- [x] Criar enum de mercados e moedas padrão.
- [x] Criar entidades de ação e histórico de cotação.
- [x] Definir constraints, precisão decimal e relacionamento.
- [x] Implementar cálculos de posição e rentabilidade.

## Integrações

- [x] Criar porta `CotacaoGateway`.
- [x] Implementar cliente brapi.
- [x] Implementar cliente Alpha Vantage.
- [x] Selecionar provedor pelo mercado.
- [x] Tratar timeout, limite e ticker inexistente.

## Aplicação

- [x] Implementar cadastro e atualização transacionais.
- [x] Impedir ticker duplicado sem diferenciar caixa.
- [x] Expor consultas paginadas e busca por ticker/id.
- [x] Criar tela de cadastro e tabela de ativos.
- [x] Integrar dados ao dashboard.

## Verificação

- [x] Testar cálculos financeiros.
- [x] Testar duplicidade e falhas externas.
- [x] Validar endpoints e interface em execução.
- [x] Documentar cenários em `spec.md`.
