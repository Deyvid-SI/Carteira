# Proposta: gestão de corretoras

## Why

O cadastro não deve aceitar qualquer empresa como corretora. É necessário validar identidade fiscal, situação cadastral, presença na CVM e endereço antes de incluir a instituição na carteira.

## What

- Validar matematicamente o CNPJ antes de chamadas externas.
- Consultar dados empresariais na BrasilAPI.
- Confirmar a instituição na base pública da CVM.
- Validar e preencher endereço pelo ViaCEP.
- Impedir CNPJ duplicado.
- Oferecer consulta paginada, por id e por CNPJ.

## Impact

Entidade `Corretora`, DTOs, service, repository, clientes BrasilAPI/ViaCEP, controller REST e tela de gestão.

## Riscos

- Bases públicas podem estar indisponíveis ou desatualizadas.
- Nomes empresariais podem variar entre Receita e CVM.
- CEP válido pode não estar cadastrado no ViaCEP.

## Critério de conclusão

Somente uma instituição com CNPJ válido, dados empresariais encontrados, confirmação CVM e CEP existente pode ser persistida.
