# Delta: carteira de ações

## ADDED Requirements

### Requirement: Aplicação protegida e orientada por dados reais

O sistema SHALL oferecer autenticação, gestão de corretoras e ações, gráficos e tabelas usando dados persistidos após validação externa.

#### Scenario: Fluxo demonstrável completo

- GIVEN um usuário autenticado e acesso às APIs configuradas
- WHEN ele cadastra uma corretora válida e um ticker existente
- THEN ambos aparecem nas tabelas
- AND os indicadores e gráficos do dashboard refletem os registros

### Requirement: Contratos de desenvolvimento

O sistema SHALL manter especificações OpenSpec no repositório e contrato OpenAPI gerado pela aplicação.

#### Scenario: Avaliação técnica

- WHEN um avaliador abre `openspec/specs` e `/swagger-ui.html`
- THEN encontra requisitos comportamentais e endpoints navegáveis correspondentes ao código entregue
