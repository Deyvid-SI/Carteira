# Administração Specification

## Purpose

Oferecer ao papel `ADMIN` uma área própria no portal para acompanhar o sistema e gerenciar contas, sem expor as carteiras dos usuários.

## Requirements

### Requirement: Acesso exclusivo

O sistema SHALL exibir o item Administração e permitir as rotas `/admin/**` somente para contas `ADMIN`.

#### Scenario: Investidor tenta abrir a área

- GIVEN uma conta autenticada com papel `USER`
- WHEN ela solicita `/admin` ou envia uma ação administrativa
- THEN a resposta é `403`
- AND nenhuma conta é alterada

#### Scenario: Menu do portal

- GIVEN uma conta `USER`
- WHEN qualquer tela do portal é exibida
- THEN o item Administração não aparece no menu

### Requirement: Privacidade das carteiras

O sistema SHALL mostrar na área de administração apenas totais do sistema, sem ativos, valores ou operações de contas específicas, e sem os valores das chaves de integração.

#### Scenario: Painel com carteiras cadastradas

- GIVEN uma conta com ativos registrados
- WHEN o administrador abre o painel e a lista de contas
- THEN o total de ativos considera esses registros
- AND nenhum ticker, empresa, valor, hash ou chave aparece nas telas

### Requirement: Gestão de contas

O sistema SHALL permitir criar contas, alterar papel, desativar, reativar e redefinir senhas, preservando pelo menos um administrador ativo.

#### Scenario: Própria conta

- GIVEN um administrador autenticado
- WHEN ele tenta rebaixar ou desativar a própria conta
- THEN a alteração é recusada com mensagem clara

#### Scenario: Último administrador ativo

- GIVEN apenas um administrador ativo
- WHEN alguém tenta rebaixá-lo ou desativá-lo pelo portal ou pela API
- THEN a alteração é recusada

### Requirement: Registro de atividades

O sistema SHALL registrar responsável, ação, conta afetada e data de cada alteração administrativa, sem senhas, hashes ou tokens.

#### Scenario: Senha redefinida

- GIVEN um administrador autenticado
- WHEN ele redefine a senha de uma conta
- THEN o registro informa a ação e a conta
- AND não contém a senha
