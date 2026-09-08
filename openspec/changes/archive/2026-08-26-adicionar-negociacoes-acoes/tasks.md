# Tasks

## 1. Domínio e persistência

- [x] 1.1 Criar enum `TipoNegociacao` com compra e venda.
- [x] 1.2 Criar entidade `Negociacao` e relacionamento obrigatório com `Acao`.
- [x] 1.3 Criar `NegociacaoRepository` com paginação e ordenação cronológica.
- [x] 1.4 Criar changeSet `002-criar-negociacoes.xml` com tabela, FK e índices.
- [x] 1.5 Incluir a migration no final do master changelog sem alterar o changeSet inicial.

## 2. Regras financeiras

- [x] 2.1 Calcular valor bruto e total com taxas.
- [x] 2.2 Recalcular preço médio ponderado nas compras.
- [x] 2.3 Manter preço médio da posição restante nas vendas.
- [x] 2.4 Calcular resultado realizado líquido de taxas.
- [x] 2.5 Zerar preço médio quando a posição for totalmente encerrada.
- [x] 2.6 Bloquear venda acima do saldo e taxas de venda acima do bruto.
- [x] 2.7 Bloquear valores inválidos e datas futuras.
- [x] 2.8 Proteger alterações concorrentes com lock pessimista.

## 3. API e interface

- [x] 3.1 Criar DTO de entrada validado e DTO de resposta.
- [x] 3.2 Criar `POST /negociacoes`.
- [x] 3.3 Criar `GET /negociacoes` paginado e ordenado.
- [x] 3.4 Criar tela `/gestao/negociacoes` com formulário.
- [x] 3.5 Exibir histórico, taxas, totais e resultado realizado.
- [x] 3.6 Adicionar compras e vendas à navegação lateral.
- [x] 3.7 Direcionar dashboard e tabela de ativos ao novo fluxo.
- [x] 3.8 Remover edição direta de posição da tabela visual.

## 4. Segurança e documentação técnica

- [x] 4.1 Remover o link OpenAPI do menu do usuário.
- [x] 4.2 Restringir Swagger, documentos OpenAPI e H2 Console ao papel `ADMIN`.
- [x] 4.3 Manter a documentação técnica gerada para avaliação administrativa.

## 5. Especificações

- [x] 5.1 Criar proposta detalhando motivação, impacto e escopo.
- [x] 5.2 Criar design com modelo, fórmulas, fluxo, decisões e riscos.
- [x] 5.3 Criar tasks por etapa com checkboxes verificáveis.
- [x] 5.4 Criar delta spec de negociações.
- [x] 5.5 Criar delta spec da restrição OpenAPI.

## 6. Verificação

- [x] 6.1 Testar compra e preço médio com taxas.
- [x] 6.2 Testar venda e resultado realizado.
- [x] 6.3 Testar rejeição de venda sem saldo.
- [x] 6.4 Testar acesso OpenAPI para `USER` e `ADMIN`.
- [x] 6.5 Testar migration Liquibase em banco vazio.
- [x] 6.6 Executar a suíte completa e confirmar zero falhas.
- [x] 6.7 Aplicar a migration sobre o H2 persistente sem perder dados.
- [x] 6.8 Validar tela e endpoints na aplicação em execução.
- [x] 6.9 Validar a mudança e todas as specs com OpenSpec strict.
