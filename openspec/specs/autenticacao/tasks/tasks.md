# Tasks: autenticação e autorização

## Cadastro

- [x] Criar entidade e repository de usuário.
- [x] Validar nome, e-mail, senha e confirmação.
- [x] Impedir e-mail duplicado sem diferenciar caixa.
- [x] Aplicar BCrypt antes de salvar.
- [x] Criar tela responsiva de cadastro.

## Sessão

- [x] Configurar `UserDetailsService`.
- [x] Criar login por formulário.
- [x] Configurar redirecionamentos de sucesso e falha.
- [x] Implementar logout e invalidação da sessão.
- [x] Proteger dashboard e páginas de gestão.

## Autorização

- [x] Suportar papéis `USER` e `ADMIN`.
- [x] Restringir recursos técnicos ao administrador.
- [x] Manter CSRF nos formulários web.
- [x] Testar visitante, usuário e administrador.

## API com JWT (Aula 13)

- [x] Adicionar `spring-boot-starter-oauth2-resource-server`.
- [x] Persistir o papel como enum `Papel` (`EnumType.STRING`).
- [x] Validar `JWT_SECRET`, `JWT_EXPIRATION` e `JWT_ISSUER` em `JwtProperties`.
- [x] Separar a cadeia stateless `/api/**` da cadeia de sessão do portal.
- [x] Emitir o token em `POST /api/auth/login` e expor `GET /api/auth/me`.
- [x] Responder `401` e `403` em `ProblemDetail`.
- [x] Testar token válido, ausente, adulterado, expirado e de outro emissor.

## Controle de acesso (Aula 14)

- [x] Habilitar `@EnableMethodSecurity`.
- [x] Documentar a matriz de acesso no README.
- [x] Criar DTOs de usuário sem senha na resposta.
- [x] Implementar `/api/usuarios` restrito a `ADMIN`, sem exclusão.
- [x] Testar `200`, `401` e `403`.

## Testes e cobertura (Aula 15)

- [x] Configurar JaCoCo (`prepare-agent`, `report`, `check`).
- [x] Medir a linha de base e versionar o portão arredondado para baixo.
- [x] Cobrir a lista de comportamentos de `UsuarioService`.
- [x] Elevar a cobertura e o portão para 80% de linhas e 80% de ramos.
- [x] Conferir a qualidade dos testes com alterações propositais no código.
