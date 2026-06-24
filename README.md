# Walletflow

Carteira digital implementada como **Monólito Modular** com Spring Modulith, explorando concorrência moderna (Virtual Threads do Java 25), arquitetura orientada a eventos e design limpo.

> Projeto de estudo/portfólio: cadastro de usuários e transferência de dinheiro entre eles, com autorização externa e notificação assíncrona. Demonstra fronteiras de módulo verificadas em build, outbox transacional e domínio rico com tipos selados.

---

## ✨ Destaques de arquitetura

- **Monólito Modular** (Spring Modulith): fronteiras de módulo *verificadas em build* — acoplamento ilegal ou cíclico quebra o `./gradlew test`.
- **Outbox transacional** (Event Publication Registry): o evento de notificação é persistido na *mesma transação* da transferência e reprocessado após crash.
- **Virtual Threads (Java 25)**: requisições e listeners assíncronos rodam em virtual threads — chamadas bloqueantes a APIs externas não esgotam o pool.
- **Autorização fora da transação**: evita segurar conexão do pool durante I/O de rede; o dinheiro só se move numa transação curta, após aprovação.
- **Domínio rico com tipos selados**: `sealed interface` + `record` + *pattern matching for switch* substituem herança pesada e cadeias de `if`.

---

## 🧱 Stack

| Camada | Tecnologia |
|---|---|
| Linguagem | Java 25 |
| Framework | Spring Boot 4.1 (Spring Framework 7) |
| Modularidade | Spring Modulith 2.1 |
| Persistência | Spring Data JPA + PostgreSQL |
| Migrations | Flyway |
| Build | Gradle 9.6 |
| Testes | JUnit 5 + Spring Modulith Test (H2) |

---

## 🗺️ Módulos

| Módulo | Responsabilidade | API pública |
|---|---|---|
| `shared` | Shared kernel: value objects `Money`, `Document` (CPF/CNPJ) | OPEN |
| `user` | Identidade: cadastro, tipos (Comum/Lojista), validações | `UserDirectory` |
| `wallet` | Saldo/dinheiro: débito/crédito com lock otimista | `WalletService` |
| `transfer` | Orquestra a transferência: valida → autoriza → move → publica evento | `TransferController`, evento `TransferCompleted` |
| `notification` | Consome o evento e chama a API instável de notificação | _nenhuma (só consome eventos)_ |

Grafo de dependência: `transfer → user, wallet`. `notification` não depende de ninguém em compile-time (só do contrato do evento). **Sem ciclos.**

```
br.com.api.walletflow
├── shared/        (Money, Document)            — OPEN
├── user/          (UserDirectory)              + internal/
├── wallet/        (WalletService)              + internal/
├── transfer/      (TransferController, evento) + internal/
└── notification/                                  internal/
```

---

## 📋 Regras de negócio

- **Usuário Comum**: envia e recebe. **Lojista**: apenas recebe.
- Campos obrigatórios e únicos: Nome Completo, CPF/CNPJ, E-mail, Senha (armazenada com hash BCrypt).
- Transferência **atômica**: rollback automático em saldo insuficiente, inconsistência ou recusa do autorizador.
- **Autorizador externo** (GET `https://util.devi.tools/api/v2/authorize`) deve aprovar **antes** do commit.
- **Notificação** (POST `https://util.devi.tools/api/v1/notify`) é **assíncrona e isolada** — instabilidade da API não afeta o fluxo principal.

---

## 🔌 API

### Cadastro de usuário
```http
POST /users
Content-Type: application/json

{
  "fullName": "Maria Silva",
  "document": "12345678901",
  "email": "maria@exemplo.com",
  "password": "secreta",
  "type": "COMMON"
}
```

### Transferência
```http
POST /transfer
Content-Type: application/json

{
  "value": 100.0,
  "payer": 4,
  "payee": 15
}
```

Fluxo: valida regras → consulta autorizador (fora da TX) → se aprovado, abre transação curta que debita/credita com lock otimista e grava o evento no outbox → notificação dispara **após o commit**, em virtual thread.

---

## 🚀 Como rodar

Pré-requisitos: **JDK 25** e **PostgreSQL** (ou ajuste o perfil para H2).

```bash
# subir Postgres local (exemplo)
docker run --name walletflow-db -e POSTGRES_DB=walletflow -e POSTGRES_PASSWORD=postgres -p 5432:5432 -d postgres:16

# rodar a aplicação
./gradlew bootRun

# build completo (compila + roda testes + verificação arquitetural)
./gradlew build
```

> Configure datasource em `src/main/resources/application.properties`. Os testes usam H2 em memória.

---

## 🧪 Testes e verificação arquitetural

```bash
./gradlew test
```

- **`ModularityTests`** → `modules.verify()`: falha o build em ciclo ou acesso a `.internal` de outro módulo. Gera diagramas C4/PlantUML em `build/`.
- **`@ApplicationModuleTest`**: sobe cada módulo isolado com mocks dos vizinhos.
- **`Scenario` / `PublishedEvents`**: valida o fluxo de eventos assíncrono (transferência → `TransferCompleted`) sem flakiness.

---

## 📚 Documentação

- **[`docs/design/architecture.md`](docs/design/architecture.md)** — design completo: Understanding Summary, Decision Log (D1–D7), arquitetura das 4 seções.
- **[`implementation-plan.md`](implementation-plan.md)** — plano de implementação incremental (T0–T6).

---

## 🛣️ Caminho de evolução (documentado, não implementado)

- Externalizar `TransferCompleted` para RabbitMQ/Kafka via `@Externalized` quando `notification` virar microsserviço — produtor inalterado.
- Idempotência de transferência via constraint `UNIQUE(idempotency_key)` (Redis só seria necessário em multi-instância).
