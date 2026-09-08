# Design: carteira de ações

## Contexto

A aplicação precisa demonstrar regras de negócio e pelo menos três integrações sem vincular o domínio aos formatos instáveis de terceiros.

## Decisões

### Arquitetura em camadas

Controllers web e REST chamam services; services usam repositories e portas Gateway. Entidades JPA não conhecem payloads externos.

### Anti-corruption layer para APIs

`EmpresaGateway`, `CvmGateway`, `CepGateway` e `CotacaoGateway` expõem registros internos pequenos. Cada cliente converte JSON de terceiro para esses registros. Essa separação atende ao objetivo extra de isolar terceiros.

### Strategy de cotações

`CotacaoGatewaySelector` escolhe a implementação pelo enum `Mercado`. Uma nova bolsa exige outro gateway, sem condicionais no serviço de ações.

### Autenticação híbrida

Form login protege a interface; HTTP Basic permite avaliar a API no Postman. Senhas são BCrypt e o CSRF é preservado nos formulários web, sendo ignorado somente nos endpoints REST autenticados.

### Gráficos locais

Os dois gráficos usam a Canvas API do navegador. Isso evita CDN e mantém o dashboard visual mesmo sem baixar biblioteca JavaScript adicional.

### Persistência

H2 em arquivo favorece a demonstração imediata. Perfis externos reutilizam o mesmo mapeamento JPA em MySQL/PostgreSQL.

## Riscos e mitigação

- Limites externos: mensagens HTTP 502, timeout e documentação das chaves.
- Dados de fechamento: o horário retornado pelo provedor é persistido e mostrado.
- Duplicidade concorrente: checagem amigável no serviço e índices únicos no banco.
- Dados sensíveis: chaves somente por variáveis/configuração, nunca no JavaScript.
