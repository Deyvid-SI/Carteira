# Tasks: gestão de corretoras

## Domínio

- [x] Criar entidade e constraint única de CNPJ.
- [x] Criar DTOs de cadastro e resposta.
- [x] Implementar algoritmo de dígitos verificadores.
- [x] Normalizar CNPJ e CEP antes da persistência.

## Integrações

- [x] Criar `EmpresaGateway` e cliente BrasilAPI.
- [x] Criar `CvmGateway` e consulta à base CVM.
- [x] Criar `CepGateway` e cliente ViaCEP.
- [x] Configurar timeouts e tratamento de indisponibilidade.

## Aplicação

- [x] Validar duplicidade antes das APIs.
- [x] Encadear as três validações externas.
- [x] Implementar consultas paginadas, por id e CNPJ.
- [x] Criar formulário e tabela de corretoras.

## Verificação

- [x] Testar CNPJ válido e inválido.
- [x] Testar ausência na CVM.
- [x] Testar duplicidade sem chamada externa.
- [x] Validar mensagens e status HTTP.
