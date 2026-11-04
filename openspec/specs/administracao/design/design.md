# Design: área de administração

## Componentes

`AdminWebController` atende `/admin` e `/admin/usuarios` e é protegido duas vezes: por `requestMatchers("/admin/**").hasRole("ADMIN")` na cadeia do portal e por `@PreAuthorize` na classe. `AdminService` monta o `PainelAdmin` só com contagens. `UsuarioService` concentra as regras das contas, usadas tanto pelo portal quanto pela API. `AtividadeAdminService` grava os registros.

## Dados

A tabela `atividades_admin` (changeset `007`) guarda responsável, ação, conta, detalhe e data. Responsável e conta são e-mails em texto, para o registro continuar legível se a conta mudar depois.

## Regras

- O administrador não altera o próprio papel nem se desativa.
- Pelo menos um administrador ativo precisa existir.
- Integrações aparecem como configuradas ou não; o valor da chave fica apenas no construtor do serviço.
- Uma conta `USER` que abre `/admin` recebe a página `error/403`.

## Testes

`AdminWebIntegrationTest` cobre acesso por papel, menu, login real, token da API, privacidade, CSRF, criação, papel, situação, senha, própria conta e último administrador. `UsuarioServiceTest`, `AdminServiceTest` e `AtividadeAdminServiceTest` cobrem as regras isoladamente.
