# Walletflow

Carteira digital implementada como Monólito Modular com Spring Modulith. Projeto de portfólio/estudo demonstrando fronteiras de módulo verificadas em build, outbox transacional, Virtual Threads (Java 25) e domínio rico com tipos selados.

## Stack

| Item | Versão |
|---|---|
| Java | 25 |
| Spring Boot | 4.1 (Spring Framework 7) |
| Spring Modulith | 2.1 |
| Spring Data JPA | via Spring Boot |
| Flyway | via Spring Boot + `flyway-database-postgresql` |
| Banco (dev/prod) | PostgreSQL 16+ |
| Banco (testes) | H2 em memória |
| Build | Gradle 9.6 (`./gradlew`) |
| Testes | JUnit 5 + Spring Modulith Test |
| Versão | Axion Release (tags git SemVer) |

## Comandos canônicos

```bash
./gradlew build          # compila + testa + verifica módulos (use sempre para confirmar task concluída)
./gradlew test           # apenas testes
./gradlew bootRun        # sobe a aplicação localmente
./gradlew release        # cria e faz push da próxima tag SemVer
```

Não há comandos de lint ou format separados — o build inclui tudo.

## Estrutura de diretórios

```
src/main/java/br/com/api/walletflow/
├── WalletflowApplication.java     ponto de entrada @SpringBootApplication
├── shared/                        shared kernel OPEN: Money, Document
├── user/                          módulo de identidade — API pública: UserDirectory
│   ├── dto/                       records de request/response
│   └── internal/                  UserEntity, UserType, UserRepository, UserService
├── wallet/                        módulo de saldo — API pública: WalletService
│   └── internal/                  WalletEntity (@Version), WalletRepository, WalletServiceImpl
├── transfer/                      orquestra transferências — TransferController, TransferCompleted
│   └── internal/                  TransferService, AuthorizationClient, IdempotencyRecord
└── notification/                  sem API pública — consome eventos
    └── internal/                  NotificationListener, NotifyClient

src/main/resources/
├── application.properties         datasource + virtual threads + modulith config
└── db/migration/                  Flyway migrations (V1–V4)

src/test/java/br/com/api/walletflow/
                                   ModularityTests + testes por módulo
```

## Convenções de código

- **DTOs como records** — imutáveis, com validações Jakarta Bean Validation
- **Injeção por construtor** — `@RequiredArgsConstructor` do Lombok
- **`@Transactional` apenas em services** (`*ServiceImpl`), nunca em controllers
- **I/O externo fora de `@Transactional`** — autorizador e notificação chamados fora de qualquer transação
- **Domínio rico fora do JPA** — entidades simples dentro de `.internal`; riqueza nos tipos selados do boundary
- **Eventos como records** — `TransferCompleted` é um record imutável e "externalization-ready"

## Nomenclatura

| Artefato | Padrão |
|---|---|
| Entidade JPA | `*Entity` — fica em `.internal` |
| DTO de entrada | `*Request` — record com Bean Validation |
| DTO de saída | `*Response` — record sem anotações de validação |
| Evento de domínio | verbo no passado — `TransferCompleted` |
| Provided interface | substantivo — `UserDirectory`, `WalletService` |
| Exceção de domínio | `*Exception` — ex: `MerchantCannotSendException` |
| Migration Flyway | `V{n}__{descricao_snake_case}.sql` |

## Módulos e dependências

```
transfer → user (via UserDirectory), wallet (via WalletService)
notification → nenhum em compile-time (só consome TransferCompleted via evento)
shared → OPEN (qualquer módulo pode usar Money e Document)
```

Ciclos e acessos a `.internal` de outro módulo **quebram o build** via `ModularityTests.verify()`.

## Restrições operacionais — o que NUNCA fazer

- Nunca edite uma migration Flyway já aplicada — crie uma nova.
- Nunca adicione `spring-boot-starter-security` — use apenas `spring-security-crypto` (ativaria a filter chain e bloquearia todos os endpoints).
- Nunca coloque chamada HTTP externa dentro de `@Transactional`.
- Nunca exponha `*Entity` fora do subpacote `.internal` do seu módulo.
- Nunca importe de `{modulo}.internal` de outro módulo.
- Nunca adicione Testcontainers — os testes usam H2 (decisão D6).
- Nunca adicione broker (Kafka/RabbitMQ) ou Redis — o caminho de evolução está documentado mas não implementado (decisão D7).
- Nunca commite credenciais reais no `application.properties`.
- Nunca use `FLOAT` ou `DOUBLE` para valores monetários — use `DECIMAL(19,2)`.
- Nunca gere SQL dinamicamente com concatenação de strings.

## Workflow de task (TDD)

Para cada task do `implementation-plan.md`:
1. Escreva o teste primeiro
2. Implemente o mínimo para passar
3. Execute `./gradlew test`
4. Refatore se necessário
5. Execute `./gradlew build` — deve ser verde antes de declarar a task concluída
6. Faça commit: `<tipo>: <descrição em pt-BR>` (máx 72 chars, sem Co-authored-by)

## Regras detalhadas

As regras completas por tema estão em:
- `.cursor/rules/java-core.mdc` — arquitetura e padrões de código
- `.cursor/rules/java-api.mdc` — contratos REST
- `.cursor/rules/java-testing.mdc` — estratégia de testes
- `.cursor/rules/java-checklist.mdc` — checklist pré-conclusão
- `.cursor/rules/commit.mdc` — padrão de commit
- `.cursor/rules/security.mdc` — segurança
- `.cursor/rules/database.mdc` — Flyway e JPA

Mesmas regras em formato Windsurf/Devin em `.devin/rules/`.

## Referências de design

- `docs/design/architecture.md` — Decision Log D1–D7, design completo
- `implementation-plan.md` — tasks T0–T6 com critérios de verificação
