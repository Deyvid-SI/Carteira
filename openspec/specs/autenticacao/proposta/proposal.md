# Proposta: autenticação e autorização

## Why

Dados financeiros e operações de carteira não podem ficar disponíveis sem identidade validada. Senhas devem ser armazenadas de modo irreversível e áreas técnicas precisam de autorização adicional.

## What

- Cadastro com nome, e-mail único e política de senha.
- Hash BCrypt antes da persistência.
- Login por formulário, sessão e logout seguro.
- JWT (Bearer Token) para clientes da API REST em `/api/**`.
- Papéis `USER` e `ADMIN` para separar uso comum e administração.

## Impact

Entidade e repository de usuário, `UsuarioService`, controllers/telas de acesso e `SecurityConfig`.

## Fora de escopo

Recuperação de senha por e-mail, MFA, OAuth social e isolamento de carteiras por proprietário.

## Critério de conclusão

Visitantes não acessam páginas protegidas; credenciais inválidas não criam sessão; senhas nunca são persistidas em texto puro.
