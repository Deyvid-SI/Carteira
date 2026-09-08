# Corretoras Specification

## Purpose

Gerenciar apenas instituições existentes e compatíveis com atuação no mercado financeiro.

## Requirements

### Requirement: Cadastro enriquecido por fontes públicas

O sistema SHALL cadastrar uma corretora somente após validar o CNPJ na Receita Federal, o registro na CVM e o CEP.

#### Scenario: Corretora válida

- GIVEN um CNPJ com dígitos verificadores válidos e presente na listagem de corretoras da CVM
- AND um CEP existente
- WHEN o cadastro é solicitado
- THEN razão social, nome fantasia, contato, situação cadastral e endereço são obtidos das APIs
- AND a corretora é persistida como validada na CVM

#### Scenario: Instituição ausente na CVM

- GIVEN uma empresa existente que não consta como corretora na CVM
- WHEN o cadastro é solicitado
- THEN o sistema impede a persistência

#### Scenario: CNPJ duplicado

- GIVEN uma corretora já cadastrada
- WHEN o mesmo CNPJ é informado novamente
- THEN o sistema rejeita a duplicidade antes de chamar serviços externos

### Requirement: Consulta de corretoras

O sistema SHALL permitir listagem paginada e busca por id ou CNPJ.

#### Scenario: Busca inexistente

- WHEN um id ou CNPJ não cadastrado é consultado
- THEN a API retorna HTTP 404 com `ProblemDetail`
