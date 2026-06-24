# Walletflow — Plano de Implementação

## Goal
Implementar o monólito modular (4 módulos + shared) conforme `docs/design/architecture.md`, mantendo `./gradlew build` verde a cada etapa.

> Ordem desenhada para o build nunca quebrar: dependências primeiro, consumidores depois, verificação por último.

## Tasks

- [x] **T0 — Setup base**: o outbox **já está cabeado** via `spring-modulith-starter-jpa` (não adicionar nada para isso). Em `application.properties` setar `spring.threads.virtual.enabled=true`, `spring.modulith.events.republish-outstanding-events-on-restart=true`, datasource Postgres + perfil de teste H2. Criar migration Flyway `V1` da tabela `event_publication` (o DDL do registry não é gerado pelo Hibernate quando se usa Flyway). → **Verify:** `./gradlew build` compila; `contextLoads` passa; tabela `event_publication` existe após migrate.

- [x] **T1 — Módulo `shared`**: `Money` (record, BigDecimal escala 2, `debit`/`credit` rejeitam negativo) e `Document` (sealed `Cpf`|`Cnpj` com validação de DV no compact constructor + factory `of(raw)`). Pacote `@ApplicationModule(type = OPEN)`. → **Verify:** testes unitários de `Money` (saldo negativo lança) e `Document` (CPF/CNPJ válido/inválido) passam.

- [x] **T2 — Módulo `user`**: `UserEntity` + `UserType` enum + `UserRepository` em `.internal`; `UserDirectory` (provided interface, lookup read-only) + `UserController` (cadastro) + DTOs record. Senha com `BCryptPasswordEncoder`. Unicidade de CPF/CNPJ e Email. Migration Flyway `V2__create_users.sql`. → **Verify:** `POST /users` cria usuário (201); duplicado retorna 409; `@ApplicationModuleTest` do `user` sobe isolado.

- [x] **T3 — Módulo `wallet`**: `WalletEntity` com `@Version` + `WalletRepository`; `WalletService` (provided interface) com `balanceOf()` e `transfer(from,to,amount)` debitando/creditando sob lock otimista. Retry em `OptimisticLockException`. Migration `V3__create_wallets.sql`. → **Verify:** teste de débito/crédito atômico; teste concorrente prova retry otimista sem saldo negativo.

- [ ] **T4 — Módulo `transfer`**: `TransferService` orquestra (1) validação com pattern matching (lojista não envia), (2) `AuthorizationClient` (`@HttpExchange` GET, fora da TX), (3) TX curta movendo dinheiro via `WalletService` + `publishEvent(TransferCompleted)`; `IdempotencyRecord` (constraint UNIQUE). `TransferController`. Migration `V4__create_transfers.sql`. → **Verify:** transferência feliz move saldo e grava evento no outbox; autorizador recusado → rollback (saldo intacto); idempotência rejeita débito duplicado.

- [ ] **T5 — Módulo `notification`**: `NotificationListener` (`@ApplicationModuleListener`) + `NotifyClient` (`@HttpExchange` POST) com `@Retryable`/backoff; tudo em `.internal`, sem API pública. → **Verify:** evento `TransferCompleted` dispara notificação após commit; falha do `notify` não reverte a transferência.

- [ ] **T6 — Verificação arquitetural (LAST)**: `ModularityTests` com `modules.verify()` + `Documenter` (gera C4/PlantUML); `TransferEventTests` com `Scenario.andWaitForEventOfType(...)`. → **Verify:** `./gradlew test` passa; `verify()` não acusa ciclo/acesso ilegal; diagramas gerados em `build/`.

## Done When
- [ ] `./gradlew build` verde com todos os testes.
- [ ] `modules.verify()` confirma 4 módulos + `shared` sem acoplamento ilegal/cíclico.
- [ ] Fluxo ponta-a-ponta: cadastro → transferência autorizada move saldo atômico → notificação assíncrona dispara; recusa/saldo insuficiente faz rollback.

## Notes
- Cada task usa TDD: teste primeiro, depois implementação (rodar `./gradlew test` ao final de cada uma).
- BCrypt: adicionar apenas `spring-security-crypto` (não o `spring-boot-starter-security`, que bloquearia todos os endpoints — incompatível com "sem login"). Ver §5 do design.
- Não introduzir broker/Redis (decisão D7) — manter `TransferCompleted` "externalization-ready" apenas.
- Referência de verdade: `docs/design/architecture.md` (Decision Log D1–D7).
