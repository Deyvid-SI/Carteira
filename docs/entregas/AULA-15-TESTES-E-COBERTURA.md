# Aula 15 — Testes de software, TDD e cobertura

Entrega do projeto **Carteira de Ações** para a Aula 15 do repositório `suporteos2025`.

## Auditoria da suíte (seção 7)

Resultado do `./mvnw clean verify` em 24/11/2026: **284 testes, 0 falhas, 0 erros**.

| Grupo | Classes de teste | Testes | Tipo predominante |
|---|---|---:|---|
| Domínio e utilitários | `AcaoCalculosTest`, `CnpjValidatorTest`, `JwtPropertiesTest` | 26 | Unitário |
| Serviços | `AcaoServiceTest`, `CorretoraServiceTest`, `NegociacaoServiceTest`, `DashboardServiceTest`, `RelatorioCarteiraServiceTest`, `UsuarioServiceTest`, `UsuarioDetailsServiceTest`, `UsuarioAtualServiceTest`, `AdminServiceTest`, `AtividadeAdminServiceTest`, `DataInitializerTest`, `CotacaoGatewaySelectorTest` | 102 | Unitário com Mockito |
| Clientes das APIs externas | `BrapiCotacaoClientTest`, `AlphaVantageCotacaoClientTest`, `BrasilApiClientTest`, `ViaCepClientTest` | 46 | Unitário com `MockRestServiceServer` |
| Autenticação e autorização | `AuthApiIntegrationTest`, `UsuarioApiIntegrationTest`, `SecurityFlowTest`, `UsuarioIsolationIntegrationTest` | 45 | Integração |
| Portal e API da carteira | `GestaoWebIntegrationTest`, `CarteiraApiIntegrationTest`, `AdminWebIntegrationTest` | 64 | Integração |
| Banco versionado | `LiquibaseMigrationTest` | 1 | Integração |

Nenhum teste acessa as APIs externas de verdade: os clientes são testados com respostas simuladas e, nos testes de integração, substituídos por dublês (`@MockitoBean`).

## Ciclos de TDD (seção 8 e atividade 4)

Cada ciclo começou pelo teste, que foi executado e falhou pelo motivo esperado antes de qualquer mudança no código de produção.

| Ciclo | Comportamento | Red (antes da implementação) | Green (depois) |
|---|---|---|---|
| A | O login é a única rota pública; não existe cadastro aberto | `SecurityFlowTest.loginDeveSerAUnicaRotaPublica`: *Range for response status value 200 expected:&lt;REDIRECTION&gt; but was:&lt;SUCCESSFUL&gt;*; `naoDeveExistirCadastroPublicoDeContas`: *Redirected URL '/login?cadastrado' does not match the expected URL pattern '\*\*/login'* (3 testes, 2 falhas) | 41 testes, 0 falhas |
| B | Conta `USER` só consulta pela API | `CarteiraApiIntegrationTest.userSomenteConsulta`: a conta `USER` chegou a executar o cadastro (*NullPointerException* ao usar a cotação simulada), em vez de receber `403` | 43 testes, 0 falhas |
| C | Conta `USER` só consulta pelo portal | `GestaoWebIntegrationTest.userSomenteConsultaNoPortal`: *Response content* (a tela ainda mostrava o formulário de cadastro) | 68 testes, 0 falhas |
| D | Token recusado assim que expira | `AuthApiIntegrationTest.deveRecusarTokenLogoDepoisDeExpirar`: *Status expected:&lt;401&gt; but was:&lt;200&gt;* | 16 testes, 0 falhas |

O ciclo D nasceu do experimento 6 da Aula 13: um token configurado para 10 s ainda era aceito 12 s depois, por causa da folga padrão de 60 s do validador do Spring Security. O teste reproduziu o problema antes da correção. Na etapa de refatoração dos ciclos B e C, os testes de escrita já existentes passaram a usar a conta `ADMIN`.

## Cobertura (seções 10 a 14)

Configuração no `pom.xml`: `jacoco-maven-plugin` 0.8.15 com `prepare-agent`, `report` e `check` na fase `verify`, e portão de **80% de linhas e 80% de ramos** (`BUNDLE`), sem nenhuma exclusão.

| Medição | Testes | Linhas | Ramos |
|---|---:|---:|---:|
| Projeto antes das aulas 13–15 | 32 | 54,9% | 32,0% |
| Depois das aulas 13 e 14 | 84 | 65,5% | 40,7% |
| Antes desta entrega | 283 | 99,5% | 95,7% |
| **Final** | **284** | **99,5%** | **96,4%** |

O portão evoluiu de 65% / 40% (piso medido, arredondado para baixo) para 80% / 80% somente depois de a suíte sustentar esse valor (atividade 9).

**Atividades 2, 3 e 6 (classe com ramos parciais).** No relatório, a `UsuarioService` tinha 2 decisões amarelas:

| Linha | Decisão | Comportamento que faltava testar |
|---|---|---|
| 74 | `request.ativo() ? "ativa" : "desativada"` | O registro de atividade quando a conta continua ativa |
| 132 | `eraAdministradorAtivo && !continuaAdministradorAtivo && ...` | Editar o cadastro do **único** administrador sem mudar papel nem situação precisa ser permitido |

O teste `UsuarioServiceTest.unicoAdministradorPodeTerOCadastroEditadoSemPerderOPapel` cobre os dois caminhos com valores de fronteira: exatamente 1 administrador ativo, papel `ADMIN` e `ativo=true`. A `UsuarioService` passou de 24/26 para 26/26 ramos, sem reduzir limites nem criar exclusões.

**Demonstração do portão (seção 14).** Com todos os testes verdes, exigir 99% de ramos reprova o build:

```text
[WARNING] Rule violated for bundle carteira-acao: branches covered ratio is 0.96, but expected minimum is 0.99
[INFO] BUILD FAILURE
```

Com a configuração versionada, o mesmo build termina com `All coverage checks have been met` e `BUILD SUCCESS`.

## Leitura do relatório HTML (seção 15)

O relatório fica em `target/site/jacoco/index.html` depois do `./mvnw clean verify`. Classes que ainda têm decisões parcialmente cobertas (amarelo):

- `DashboardService`: 28/31 ramos
- `GestaoWebController`: 15/18 ramos
- `AdminWebController`: 7/8 ramos
- `ApiExceptionHandler`: 1/2 ramos
- `AuthController`: 1/2 ramos
- `BrapiCotacaoClient`: 41/42 ramos
- `RelatorioCarteiraService`: 9/10 ramos

São decisões defensivas ou ramos que o fluxo real não alcança. Exemplo: em `AuthController`, o caminho de visitante sem login nunca chega ao controller, porque o Spring Security redireciona antes. Escrever testes só para pintar essas linhas de verde iria contra a regra da aula.

## Testes que detectam alterações propositais (seção 19 e atividade 8)

O código foi alterado de propósito numa **cópia** do projeto, e a classe de teste correspondente foi executada:

| Alteração proposital | Classe de teste executada | Resultado | Teste que falhou |
|---|---|---|---|
| Remove @PreAuthorize do cadastro de ações na API | `CarteiraApiIntegrationTest` | detectada | `userSomenteConsulta` |
| Remove @PreAuthorize do registro de negociação no portal | `GestaoWebIntegrationTest` | detectada | `userSomenteConsultaNoPortal` |
| Reabre /cadastro como rota pública | `SecurityFlowTest` | detectada | `loginDeveSerAUnicaRotaPublica` |
| Volta a folga de 60 s na expiração do JWT | `AuthApiIntegrationTest` | detectada | `deveRecusarTokenLogoDepoisDeExpirar` |
| Remove @PreAuthorize da administração de usuários | `UsuarioApiIntegrationTest` | detectada | `userNaoDeveGerenciarUsuarios` |
| Venda permitida acima do saldo (> vira >=) | `NegociacaoServiceTest` | detectada | `vendaDeTodoOSaldoDeveZerarOPrecoMedio` |
| Chave JWT de 32 bytes passa a ser recusada | `JwtPropertiesTest` | detectada | `chaveCom32BytesEhAceita` |

As **7** alterações foram detectadas: em cada uma, pelo menos um teste falhou, e o arquivo original foi restaurado antes da próxima. As quatro primeiras cobrem as regras adicionadas nesta entrega; a quinta, a proteção da administração de usuários. A amostra foi escolhida manualmente e não substitui uma ferramenta de mutação como o PIT, que a aula sugere para módulos críticos ou para uma rotina noturna de CI.

## Uso de dublês (seção 17 e atividade 7)

| Dublê | Onde | Por quê |
|---|---|---|
| Repositórios simulados (Mockito) | Testes unitários de serviços | Isolam a regra do serviço do banco, e o teste roda em milissegundos |
| `PasswordEncoder` simulado | `UsuarioServiceTest` | Deixa o resultado previsível (`hash-de-...`) e evita o custo do BCrypt em cada teste |
| `MockRestServiceServer` | Testes dos clientes brapi, Alpha Vantage, BrasilAPI e ViaCEP | Simula respostas e falhas HTTP sem depender da internet nem dos limites dessas APIs |
| `@MockitoBean` dos clientes externos | `GestaoWebIntegrationTest`, `CarteiraApiIntegrationTest` | Mantém MVC, validação, segurança, Liquibase e H2 reais, trocando só a fronteira externa |
| `@WithMockUser` e `user(...)` | Testes de autorização | Focam na regra de papel sem repetir o login; o JWT real é testado à parte em `AuthApiIntegrationTest` |

Não há mock de DTOs nem da própria classe testada.

## Integração contínua (seção 20)

`.github/workflows/testes.yml` executa `./mvnw --batch-mode clean verify` em todo push e pull request, com Java 17, e guarda `target/site/jacoco/` como artefato `relatorio-jacoco`.

## Checklist (seção 22)

| Item | Situação |
|---|:---:|
| Todos os testes estão verdes | ok |
| `./mvnw clean verify` termina com `BUILD SUCCESS` | ok |
| O relatório HTML foi aberto e interpretado | ok |
| Cobertura de linhas permanece em pelo menos 80% | ok |
| Cobertura de ramos não fica abaixo do portão versionado | ok |
| Testes possuem asserções relevantes | ok |
| Caminhos de sucesso e erro foram considerados | ok |
| Mocks representam dependências, não a classe testada | ok |
| Nenhum limite foi reduzido para fazer o build passar | ok |
| Nenhuma exclusão foi adicionada sem justificativa | ok |
