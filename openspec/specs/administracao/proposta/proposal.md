# Proposta: área de administração

## Why

O papel `ADMIN` só tinha acesso à gestão de usuários pela API e às ferramentas técnicas por URL. Faltava um lugar no portal para acompanhar o sistema e administrar contas sem quebrar a privacidade das carteiras.

## What

- Item Administração no menu, visível apenas para `ADMIN`.
- Painel com totais do sistema, atividades recentes, situação das integrações e atalhos técnicos.
- Tela de contas para criar, promover, rebaixar, desativar, reativar e redefinir senha.
- Registro persistido das atividades administrativas.

## Fora do escopo

- Ver ou alterar a carteira de outra conta.
- Excluir contas.
- Encerrar sessões ou revogar tokens já emitidos.

## Critério de conclusão

Investidor recebe `403` em todas as rotas `/admin/**`, nenhuma tela administrativa expõe dados de carteira e todas as alterações de conta ficam registradas.
