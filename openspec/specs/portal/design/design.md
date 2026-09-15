# Design: portal do investidor

## Rotas

```text
GET /configuracoes  → PortalWebController → configuracoes.html
GET /historico      → RelatorioCarteiraService → historico.html
GET /imposto-renda  → RelatorioCarteiraService → imposto-renda.html
```

As rotas herdam a regra `anyRequest().authenticated()` do Spring Security.

## Histórico

`NegociacaoRepository` carrega todas as operações em ordem decrescente com `EntityGraph` para evitar consultas tardias da ação. O servidor calcula os totais; busca e filtros apenas controlam a visibilidade das linhas já renderizadas.

## Consolidação anual

O valor de vendas considera somente operações `VENDA`. Resultado realizado usa o valor já calculado no registro da negociação. Os campos de BRL e USD são independentes e nenhuma conversão implícita é feita.

O relatório é deliberadamente um apoio organizacional. O modelo atual não armazena classificação day trade, retenções, custos tributários completos, câmbio oficial ou eventos corporativos; por isso a interface não calcula imposto devido.

## Preferências

`theme.js` lê e salva `carteira-theme`, `carteira-motion` e `carteira-density` no `localStorage`. A configuração não envia informações pessoais ao servidor e pode ser restaurada ao padrão.

## Interface

Os três templates reutilizam navegação, tokens de cor, cartões, tabelas e responsividade existentes. A textura `marble-black-gold.png` é um ativo original do projeto e recebe sobreposição escura para preservar contraste.

## Testes

- Testes unitários validam agregação por ano, tipo e moeda.
- MockMvc valida as três views, a nova navegação e o acesso público ao ativo estático.
- Verificação visual cobre tema escuro, tema claro, rolagem das abas e estados vazios.
