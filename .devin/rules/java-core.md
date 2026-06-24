---
trigger: always_on
description: Arquitetura, estrutura de módulos, padrões de código e nomenclatura do Walletflow
---

# Walletflow — Java Core

## Estrutura de pacotes

O pacote-base é `br.com.api.walletflow`. Cada módulo do Spring Modulith ocupa um subpacote direto. Tudo em `.internal` é invisível para os demais módulos — essa restrição é **verificada em build** pelo `ModularityTests`.

```
br.com.api.walletflow
├── WalletflowApplication.java
├── shared/                   @ApplicationModule(type = OPEN)
│   ├── Money.java            record, BigDecimal escala 2
│   └── Document.java         sealed: Cpf | Cnpj
├── user/
│   ├── UserController.java
│   ├── UserDirectory.java    provided interface — único ponto de acesso externo
│   ├── dto/                  records de request/response
│   └── internal/
│       ├── UserEntity.java · UserType.java · UserRepository.java
│       └── UserService.java  implements UserDirectory
├── wallet/
│   ├── WalletService.java    provided interface: balanceOf(), transfer()
│   └── internal/
│       ├── WalletEntity.java (@Version para lock otimista)
│       ├── WalletRepository.java
│       └── WalletServiceImpl.java
├── transfer/
│   ├── TransferController.java
│   ├── TransferCompleted.java  evento de domínio (record)
│   └── internal/
│       ├── TransferService.java
│       ├── AuthorizationClient.java  @HttpExchange
│       └── IdempotencyRecord.java
└── notification/
    └── internal/
        ├── NotificationListener.java  @ApplicationModuleListener
        └── NotifyClient.java          @HttpExchange
```

## Regras de módulo (invioláveis)

- Outros módulos acessam `user` **somente** via `UserDirectory`; `wallet` somente via `WalletService`.
- Nenhuma entidade JPA (`*Entity`) cruza fronteiras de módulo — mapeie para interface/record no boundary.
- `notification` não tem API pública. Zero imports dela em outros módulos em compile-time.
- `shared` é OPEN: `Money` e `Document` podem ser usados em qualquer módulo sem restrição do Modulith.
- Adicionar dependência entre módulos exige atualização do `architecture.md` (Decision Log).

## Padrões de código Java 25

### Value Objects como records
```java
public record Money(BigDecimal amount) {
    public Money {
        Objects.requireNonNull(amount);
        if (amount.scale() > 2) amount = amount.setScale(2, RoundingMode.HALF_UP);
    }
    public Money debit(Money other) {
        var result = this.amount.subtract(other.amount);
        if (result.compareTo(BigDecimal.ZERO) < 0) throw new InsufficientBalanceException();
        return new Money(result);
    }
}
```

### Sealed interfaces + pattern matching
```java
public sealed interface Account permits CommonUser, Merchant { Document document(); }

switch (payer) {
    case Merchant m    -> throw new MerchantCannotSendException(m.id());
    case CommonUser c  -> { /* ok */ }
}
```

### Injeção de dependências
Sempre por construtor. Use `@RequiredArgsConstructor` do Lombok em classes que não são records.

### Transações
- `@Transactional` **apenas em services** (`*ServiceImpl`), nunca em controllers ou repositories.
- Chamadas HTTP externas (`AuthorizationClient`, `NotifyClient`) fora de qualquer `@Transactional`.
- Transação de transferência deve ser curta: debitar + creditar + publishEvent — sem I/O externo dentro.

### Virtual Threads (Java 25)
`spring.threads.virtual.enabled=true` habilita virtual threads em Tomcat e nos executores `@Async`. Não crie `ThreadLocal` de longa duração — prefira parâmetros explícitos ou `ScopedValue`.

## Nomenclatura

| Artefato | Convenção | Exemplo |
|---|---|---|
| Entidade JPA | `*Entity` | `UserEntity` |
| DTO de entrada | `*Request` | `CreateUserRequest` |
| DTO de saída | `*Response` | `UserResponse` |
| Evento de domínio | verbo passado | `TransferCompleted` |
| Provided interface | substantivo | `UserDirectory`, `WalletService` |
| Exceção de domínio | `*Exception` | `MerchantCannotSendException` |
| Migration Flyway | `V{n}__{descricao}.sql` | `V2__create_users.sql` |

## Módulos relacionados

- `java-api.md` — contratos REST
- `java-testing.md` — estratégia de testes
- `java-checklist.md` — checklist pré-tarefa
- `database.md` — Flyway e JPA
