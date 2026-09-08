# Proposta: adicionar compras e vendas de ações

## Why

A carteira armazenava somente uma quantidade e um preço médio editáveis. Esse modelo não explicava como a posição foi formada, não registrava entradas e saídas, não calculava resultado realizado e permitia alterar o saldo sem trilha histórica.

A navegação também mostrava um acesso OpenAPI destinado à avaliação técnica. Essa opção não pertence ao fluxo cotidiano de um investidor e deve ficar invisível e inacessível para usuários comuns.

## What Changes

- Criar o domínio de negociação com operações `COMPRA` e `VENDA`.
- Persistir ativo, quantidade, preço unitário, taxas, valor bruto, valor líquido, resultado realizado e data da operação.
- Recalcular quantidade e preço médio automaticamente a cada compra.
- Reduzir o saldo e calcular lucro ou prejuízo realizado a cada venda.
- Impedir vendas acima da posição disponível e validar datas e valores.
- Usar bloqueio pessimista no ativo para evitar duas vendas simultâneas sobre o mesmo saldo.
- Criar API REST paginada para registrar e consultar negociações.
- Criar tela responsiva com formulário e histórico das movimentações.
- Remover OpenAPI da navegação e restringir Swagger e documentos API ao papel `ADMIN`.
- Adicionar um novo changeSet Liquibase sem modificar migrations já executadas.

## Capabilities

- `negociacoes`
- `api-openapi`

## Impact

### Código

- Nova entidade, enum, DTOs, repository, service, controller REST e template web.
- Alteração do cadastro de ativo para iniciar com saldo zero quando não houver posição informada.
- Alteração da segurança e da navegação lateral.

### Banco de dados

- Nova tabela `negociacoes` relacionada a `acoes`.
- Dois índices para histórico por ativo/data e filtro por tipo.
- Nenhum dado existente é apagado ou reescrito.

### Compatibilidade

- Os endpoints atuais permanecem disponíveis.
- A atualização manual de posição permanece no contrato legado, mas deixa de aparecer na interface principal.
- Usuários com papel `USER` recebem HTTP 403 ao tentar abrir Swagger ou `/v3/api-docs`.

## Fora de escopo

- Integração com ordens reais de corretoras.
- Apuração tributária, DARF, notas de corretagem e desdobramentos.
- Conversão automática entre BRL e USD.
- Cancelamento ou edição retroativa de uma negociação já registrada.
