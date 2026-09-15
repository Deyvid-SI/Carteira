# Design: isolamento das carteiras

## Modelo de dados

`acoes.usuario_id` e `corretoras.usuario_id` são chaves estrangeiras obrigatórias para `usuarios.id`. As restrições compostas `(usuario_id, ticker)` e `(usuario_id, cnpj)` substituem a unicidade global.

Negociações e históricos não duplicam o usuário: a propriedade é obtida pelo relacionamento obrigatório com a ação, evitando divergência entre duas colunas de proprietário.

## Autorização

`UsuarioAtualService` resolve a conta pelo e-mail autenticado no Spring Security. Os repositórios recebem o identificador do usuário em buscas, paginações, contagens e no bloqueio pessimista usado durante compras e vendas.

Um identificador existente em outra carteira produz a mesma resposta de recurso ausente, sem confirmar sua existência.

## Migração

O Liquibase associa registros legados à conta de menor identificador antes de tornar a coluna obrigatória. Contas criadas depois da migração permanecem vazias. A migração de quantidade interrompe com uma mensagem clara se encontrar algum saldo legado fracionário, evitando arredondar ou apagar frações silenciosamente.

## Testes

Os testes de integração criam duas contas, persistem posições distintas e verificam tela, dashboard e API. Testes unitários confirmam que serviços passam o usuário atual às consultas.
