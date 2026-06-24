# Walletflow — Documento de Design Arquitetural

> Monólito Modular (Spring Modulith) · Java 25 · Spring Boot 4.1 (Spring Framework 7) · Gradle 9.6 · PostgreSQL · Flyway
>
> Status: **Design aprovado** (brainstorming concluído) · Data: 2026-06-24

---

## 1. Understanding Summary

- **O que estamos construindo:** um monólito modular de carteira digital com cadastro de usuários e transferência de dinheiro entre eles, exercitando concorrência moderna (Virtual Threads), arquitetura orientada a eventos e código limpo.
- **Por que existe:** projeto-vitrine de arquitetura de estado da arte (carteira digital com transferências) para portfólio / entrevista técnica.
- **Para quem:** o autor (Carlos) como estudo/portfólio e avaliadores técnicos.
- **Topologia:** 4 módulos de domínio com fronteiras explícitas e verificadas em build — `user`, `wallet`, `transfer`, `notification` — mais um shared kernel `shared`.
- **Restrições centrais:** transferência atômica com rollback; autorizador externo deve aprovar antes do commit; notificação assíncrona e isolada sobre uma API instável.

### Regras de negócio
- Dois tipos de usuário: **Comum** (envia e recebe) e **Lojista** (apenas recebe).
- Campos obrigatórios e únicos: Nome Completo, CPF/CNPJ, Email, Senha.
- Transferência **atômica**: rollback automático em inconsistência (saldo insuficiente) ou recusa do autorizador.
- Notificação **assíncrona e isolada**, sem impactar o fluxo principal.

### Não-objetivos (explícitos)
- Sem login/JWT, sem extrato/histórico como feature, sem frontend, sem multi-moeda, sem i18n.
- Sem broker (Kafka/RabbitMQ) e sem Redis nesta versão — apenas caminho de evolução documentado.

---

## 2. Assumptions (defaults confirmados)

| # | Assunção | Status |
|---|----------|--------|
| A1 | Virtual Threads habilitadas (`spring.threads.virtual.enabled=true`) para servidor web e executor async. | Confirmado |
| A2 | Consistência de saldo via **lock otimista** (`@Version`) com retry — não pessimista. | Confirmado |
| A3 | Escala de estudo/demo; Postgres único, sem sharding. | Confirmado |
| A4 | Resiliência da notificação: retry com backoff + sobrevivência a crash via outbox; sem dead-letter externo. | Confirmado |
| A5 | Cliente HTTP: `RestClient` declarativo (`@HttpExchange` + `HttpServiceProxyFactory`) do Spring 7, timeouts curtos. | Confirmado |
| A6 | Testes: **H2** para slices de módulo e para o teste de outbox (sem Testcontainers). | Confirmado |

---

## 3. Decision Log

| # | Decisão | Alternativas consideradas | Por quê |
|---|---------|---------------------------|---------|
| D1 | **`wallet` é módulo próprio** (4 módulos). | Saldo dentro de `user`; saldo dentro de `transfer`. | Separa identidade (quem é) de dinheiro (quanto tem); melhor DDD e melhor exercício das fronteiras do Modulith. |
| D2 | **Outbox transacional** via `spring-modulith-starter-jpa` (Event Publication Registry — **já presente** no `build.gradle`) + republicação de eventos pendentes no restart. | Evento em memória + retry; outbox sem republish. | Evento persiste na mesma TX da transferência; sobrevive a crash → resolve de fato a "instabilidade" da notificação. |
| D3 | **Sem login/JWT**; senha apenas com hash **BCrypt** como dado obrigatório. | JWT stateless; sessão Spring Security. | Mantém escopo enxuto (YAGNI); foco em Modulith/concorrência, não em segurança transversal. |
| D4 | **Autorizar fora da TX**, mover dinheiro em transação curta. | Tudo dentro de uma `@Transactional`. | Evita segurar conexão do pool durante I/O de rede; re-checa saldo dentro da TX com lock otimista. |
| D5 | Lock **otimista** (`@Version`) com retry para o saldo. | Lock pessimista (`SELECT ... FOR UPDATE`). | Melhor concorrência sob a carga de estudo; evita contención de banco. |
| D6 | **H2** nos testes. | Testcontainers PostgreSQL. | Simplicidade; sem dependência de Docker no ciclo de teste. |
| D7 | **Monólito limpo + caminho de evolução documentado** (sem broker/Redis agora). | RabbitMQ agora; Kafka agora; Redis para idempotência. | Outbox in-process já entrega async durável; `@Externalized` deixa a graduação para broker pronta. Idempotência via constraint UNIQUE no Postgres. |

---

## 4. Final Design

### 4.1 Arquitetura de Módulos (Spring Modulith)

Regra de ouro: **o pacote-base de cada módulo é a API pública; tudo em `.internal` é invisível para os demais módulos** (verificado em build).

```
br.com.api.walletflow
├── WalletflowApplication.java      // @SpringBootApplication (raiz)
│
├── shared/                          // shared kernel — @ApplicationModule(type = OPEN)
│   ├── Money.java                   // record (BigDecimal + escala/validação)
│   └── Document.java                // sealed: Cpf | Cnpj
│
├── user/                            // API pública do módulo
│   ├── UserController.java
│   ├── UserDirectory.java           // PROVIDED INTERFACE (lookup read-only p/ outros módulos)
│   ├── dto/  (records de request/response)
│   └── internal/                    // escondido
│       ├── UserEntity.java  ·  UserType.java(enum)  ·  UserRepository.java
│       └── UserService.java         // impl de UserDirectory + cadastro/validação
│
├── wallet/
│   ├── WalletService.java           // PROVIDED INTERFACE: balanceOf(), transfer(from,to,amount)
│   ├── WalletCreated.java           // (evento, se necessário)
│   └── internal/
│       ├── WalletEntity.java (@Version p/ lock otimista)  ·  WalletRepository.java
│       └── WalletServiceImpl.java
│
├── transfer/
│   ├── TransferController.java
│   ├── TransferCompleted.java       // EVENTO de domínio (record) — "externalization-ready"
│   └── internal/
│       ├── TransferService.java     // orquestra: autoriza → TX curta → publica evento
│       ├── AuthorizationClient.java // @HttpExchange (RestClient declarativo)
│       └── IdempotencyRecord.java   // constraint UNIQUE(idempotency_key)
│
└── notification/                    // SÓ internal — não expõe nada (só consome eventos)
    └── internal/
        ├── NotificationListener.java   // @ApplicationModuleListener
        └── NotifyClient.java           // @HttpExchange p/ a API instável
```

**Conformidade:**
- `user`, `wallet`, `transfer` expõem **provided interfaces** finas (`UserDirectory`, `WalletService`); outros módulos dependem de interface, nunca de entidade JPA.
- `notification` **não tem API pública** — desacoplamento total; só reage a eventos (isolamento da API instável).
- `shared` como módulo **OPEN** evita reclamações do Modulith sobre dependências para os value objects.
- Grafo de dependência: `transfer → user, wallet`; `notification` não depende de ninguém em compile-time (só do contrato do evento). **Sem ciclos.**

### 4.2 Eventos Assíncronos + Virtual Threads (Java 25)

**Fluxo da transferência** (decisão D4):

```
TransferController
   └─> TransferService.transfer(cmd)
        1. valida regras de negócio (pattern matching — 4.3)        ← fora da TX
        2. AuthorizationClient.authorize()  (GET externo, RestClient) ← fora da TX
        3. se recusado → lança TransferDeniedException (nada tocou o banco)
        4. @Transactional  ───────────── TX curta ─────────────────┐
             walletService.transfer(payer, payee, amount)          │
                → débito/crédito com @Version (lock otimista)       │
             eventPublisher.publishEvent(new TransferCompleted(...))│  ← gravado no
        5. commit ──────────────────────────────────────────────────┘     OUTBOX (mesma TX)
```

- O evento `TransferCompleted` é persistido na **Event Publication Registry** (fornecida por `spring-modulith-starter-jpa`, já no `build.gradle`) **dentro da mesma transação** do dinheiro → atomicidade real.
- `spring.modulith.events.republish-outstanding-events-on-restart=true` reprocessa eventos pendentes no boot (sobrevive a crash).

**Consumo assíncrono e isolado:**

```java
// notification/internal/NotificationListener
@ApplicationModuleListener   // = @Async + @TransactionalEventListener(AFTER_COMMIT) + @Transactional(REQUIRES_NEW)
void on(TransferCompleted e) { notifyClient.notify(...); }  // chamada bloqueante à API instável
```

- `AFTER_COMMIT`: notifica só após o dinheiro commitado → nunca notifica transferência revertida.
- `@Async`: roda fora da thread da requisição → pagador recebe `200` sem esperar a API instável.
- Transações separadas: falha/timeout do `notify` **não** reverte a transferência (isolamento).

**Virtual Threads (Java 25):** `spring.threads.virtual.enabled=true`
1. **Tomcat** atende cada request numa virtual thread → bloqueio no `authorize()` não consome platform thread do pool.
2. O executor de `@Async`/listeners passa a usar virtual threads → cada notificação lenta bloqueia uma VT baratíssima; milhares de notificações pendentes não esgotam o pool.

**Resiliência:** retry com backoff no listener (`@Retryable`/`RetryTemplate`); esgotadas as tentativas, o registro fica incompleto no outbox e é recuperável.

**Ponto "externalization-ready":** quando `notification` sair do processo, anotar `TransferCompleted` com `@Externalized(...)` + `spring-modulith-events-amqp`/`-kafka` — zero mudança no produtor (decisão D7).

### 4.3 Design Patterns Modernos (Java 25)

Princípio: **JPA simples no `.internal`; riqueza de domínio em tipos selados imutáveis no boundary.**

**Tipo de usuário — `sealed interface` + pattern matching:**

```java
public sealed interface Account permits CommonUser, Merchant {
    Document document();
}
public record CommonUser(UUID id, Document document) implements Account {}
public record Merchant(UUID id, Document document)   implements Account {}
```

Regra "lojista só recebe" via **switch exaustivo** (build quebra se surgir novo tipo não tratado):

```java
switch (payer) {
    case Merchant m   -> throw new MerchantCannotSendException(m.id());
    case CommonUser c -> { /* ok, segue */ }
}
```

**`Document` — `sealed` CPF/CNPJ com validação no compact constructor:**

```java
public sealed interface Document permits Cpf, Cnpj {
    String value();
    static Document of(String raw) { /* decide Cpf|Cnpj por tamanho/dígitos */ }
}
public record Cpf(String value)  implements Document { public Cpf { /* valida 11 díg + DV */ } }
public record Cnpj(String value) implements Document { public Cnpj { /* valida 14 díg + DV */ } }
```

- **`Money`** como record value object: encapsula `BigDecimal` + escala + `debit`/`credit` que rejeitam negativo → elimina classes inteiras de bug (ponto-flutuante, saldo negativo).
- **Eventos e DTOs** como `record`: imutáveis, concisos, serializáveis.

**Ponte com JPA:** `UserEntity` (`.internal`) guarda `UserType` **enum** + colunas planas (sem herança de tabela). `UserService` mapeia entity → `Account` selado ao cruzar o boundary → persistência trivial + domínio rico, sem `@Inheritance`.

### 4.4 Verificação Arquitetural (falha o build no Gradle)

```java
class ModularityTests {
    static final ApplicationModules modules = ApplicationModules.of(WalletflowApplication.class);

    @Test void semCiclosNemAcessoIlegal() {
        modules.verify();   // falha se: ciclo, acesso a .internal alheio, ou dependência não declarada
    }

    @Test void documentacao() {
        new Documenter(modules)
            .writeDocumentation()              // diagramas C4/PlantUML
            .writeIndividualModulesAsPlantUml();
    }
}
```

- `modules.verify()` quebra o `test` (e portanto o `build`) em qualquer violação de fronteira.
- `@ApplicationModuleTest`: bootstrap isolado de um módulo + mocks dos contratos vizinhos (prova autonomia).
- `Scenario`/`PublishedEvents`: valida o fluxo de eventos async sem flakiness:

```java
@ApplicationModuleTest
class TransferEventTests {
    @Test void publicaTransferCompletedAposSucesso(Scenario scenario) {
        scenario.stimulate(() -> transferService.transfer(cmd))
                .andWaitForEventOfType(TransferCompleted.class)
                .toArriveAndVerify(evt -> { /* asserts */ });
    }
}
```

- Integração Gradle: `spring-modulith-starter-test` já está no `build.gradle`; `tasks.named('test')` existente roda tudo. Em CI, o `build` falha em qualquer violação.

---

## 5. Dependências (estado real vs. a adicionar)

**Já presentes no `build.gradle` (verificado):**
- ✅ **Outbox/Event Publication Registry** — `spring-modulith-starter-jpa` já fornece a infraestrutura. **Nenhuma dependência a adicionar** para o outbox (correção: não existe artefato `spring-modulith-starter-events-jpa`; o starter por persistência é o `-jpa`).
- ✅ Cliente HTTP declarativo (`RestClient` + `@HttpExchange`) via `spring-boot-starter-webmvc` (A5).
- ✅ `spring-modulith-starter-test` (testes de modularidade), `flyway` + `flyway-database-postgresql`, `data-jpa`, `validation`, H2 + PostgreSQL.

**A adicionar:**
- `org.springframework.security:spring-security-crypto` — **apenas** o `BCryptPasswordEncoder` (D3). Preferir isto a `spring-boot-starter-security`, que ativaria a filter chain e bloquearia todos os endpoints (incompatível com "sem login").

**A configurar:**
- `application.properties`: `spring.threads.virtual.enabled=true` (A1) e `spring.modulith.events.republish-outstanding-events-on-restart=true` (D2). _Nome da propriedade verificado na doc oficial do Modulith._
- **Migration Flyway para a tabela `event_publication`** do registry (com Flyway, o DDL não é gerado por Hibernate). Sem isso, o outbox não persiste. Criar junto com a primeira migration.

---

## 6. Próximo passo

Design validado e travado. Quando autorizado, gerar o **plano de implementação** incremental (módulo a módulo, com TDD e verificação do Modulith a cada etapa).
