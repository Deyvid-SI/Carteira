# Autenticação Specification

## Purpose

Proteger dados e operações da carteira por uma sessão de usuário validada.

## Requirements

### Requirement: Cadastro validado

O sistema SHALL permitir cadastro com nome, e-mail único e senha de 8 a 72 caracteres contendo letra maiúscula, minúscula e número.

#### Scenario: Cadastro válido

- GIVEN um e-mail ainda não utilizado
- WHEN o usuário informa dados válidos e duas senhas iguais
- THEN a senha é armazenada como hash BCrypt
- AND o usuário pode autenticar-se

#### Scenario: E-mail duplicado

- GIVEN um e-mail já cadastrado
- WHEN um novo cadastro usa o mesmo e-mail sem diferenciar maiúsculas
- THEN o sistema rejeita o cadastro com mensagem clara

### Requirement: Controle de acesso

O sistema SHALL exigir autenticação para dashboard, tabelas e endpoints de negócio.

#### Scenario: Visitante acessa área protegida

- GIVEN um visitante sem sessão
- WHEN ele solicita uma página de gestão
- THEN é direcionado ao login

#### Scenario: Credenciais inválidas

- GIVEN uma combinação de e-mail e senha incorreta
- WHEN o login é enviado
- THEN nenhuma sessão autenticada é criada
- AND a tela informa que as credenciais são inválidas
