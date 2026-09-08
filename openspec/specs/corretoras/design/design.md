# Design: gestão de corretoras

## Fluxo de validação

```text
DTO → CnpjValidator → duplicidade → EmpresaGateway → CvmGateway → CepGateway → persistência
```

A ordem reduz chamadas desnecessárias: formato e duplicidade são resolvidos localmente antes das integrações.

## Portas externas

- `EmpresaGateway`: razão social, nome fantasia, contato e situação cadastral.
- `CvmGateway`: confirmação de registro e status perante a CVM.
- `CepGateway`: logradouro, bairro, cidade e UF.

Clientes externos convertem JSON para registros internos pequenos. O service não conhece a estrutura original das APIs.

## Persistência

CNPJ é armazenado somente com 14 dígitos e possui constraint única. `validadaNaCvm` e `statusCvm` preservam a evidência usada no cadastro.

## Regras

- Dígitos verificadores inválidos encerram o fluxo.
- Duplicidade é verificada sem nova consulta externa.
- Empresa ausente ou instituição não reconhecida pela CVM não é salva.
- CEP inexistente impede cadastro incompleto.

## Erros

Regra de negócio retorna HTTP 422; indisponibilidade externa retorna HTTP 502; busca sem resultado persistido retorna HTTP 404.

## Testes

Testes unitários cobrem algoritmo de CNPJ, duplicidade precoce, ordem dos gateways e falhas de validação.
