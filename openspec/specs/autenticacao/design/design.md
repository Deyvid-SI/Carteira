# Design: autenticação e autorização

## Componentes

`UsuarioService` valida cadastro e usa `PasswordEncoder`. `UserDetailsService` adapta `Usuario` ao Spring Security. `SecurityFilterChain` define rotas públicas, autenticadas e administrativas.

## Política de senha

- 8 a 72 caracteres;
- ao menos uma maiúscula, uma minúscula e um número;
- confirmação idêntica;
- BCrypt para armazenamento.

## Fluxos

### Cadastro

Validar DTO → normalizar e-mail → rejeitar duplicidade → aplicar BCrypt → persistir usuário ativo.

### Login

Spring Security consulta por e-mail, compara o hash e cria sessão. Sucesso direciona ao dashboard; falha retorna à tela com mensagem genérica.

### Logout

Invalida sessão, remove cookie `JSESSIONID` e retorna ao login.

## Matriz de acesso

| Recurso | Visitante | USER | ADMIN |
|---|---:|---:|---:|
| Login/cadastro/assets estáticos | sim | sim | sim |
| Dashboard e gestão | não | sim | sim |
| Swagger/OpenAPI/H2 Console | não | não | sim |

## Segurança

CSRF permanece ativo nos formulários web. Endpoints REST autenticados são ignorados pelo CSRF para uso com HTTP Basic.
