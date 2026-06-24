---
trigger: always_on
description: Padrões de banco de dados, Flyway migrations e JPA para o Walletflow
---

# Walletflow — Banco de Dados

## Dois perfis de banco

| Contexto | Banco | Configuração |
|---|---|---|
| Produção / dev local | PostgreSQL 16+ | `application.properties` com datasource |
| Testes automatizados | H2 em memória | Sobe automaticamente com `@ApplicationModuleTest` |

Não use Testcontainers — a decisão D6 do design define H2 para simplicidade no ciclo de testes.

## Flyway migrations

- Arquivo em `src/main/resources/db/migration/`
- Nomenclatura: `V{n}__{descricao_em_snake_case}.sql`
- **Nunca edite uma migration já aplicada** — crie uma nova se precisar corrigir
- Migrations são atômicas: uma migration deve ou commitar completa ou falhar sem efeito colateral

### Migrations planejadas

| Versão | Conteúdo |
|---|---|
| `V1__create_event_publication.sql` | Tabela do outbox do Spring Modulith (DDL não gerado pelo Hibernate com Flyway) |
| `V2__create_users.sql` | Tabela `users` com CPF/CNPJ, email únicos, senha hash |
| `V3__create_wallets.sql` | Tabela `wallets` com `version` para lock otimista |
| `V4__create_transfers.sql` | Tabela `transfers` com `idempotency_key` UNIQUE |

### Convenções SQL

```sql
-- Nomenclatura: snake_case, tabelas no plural
CREATE TABLE users (
    id          BIGSERIAL PRIMARY KEY,
    full_name   VARCHAR(255) NOT NULL,
    document    VARCHAR(14)  NOT NULL UNIQUE,  -- CPF ou CNPJ
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,         -- BCrypt hash
    type        VARCHAR(20)  NOT NULL,         -- COMMON | MERCHANT
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

-- Índice explícito em FK
CREATE INDEX idx_wallets_user_id ON wallets(user_id);

-- Constraint nomeada
ALTER TABLE transfers ADD CONSTRAINT uk_transfers_idempotency_key UNIQUE (idempotency_key);
```

- Valores monetários: `DECIMAL(19, 2)` — nunca `FLOAT` ou `DOUBLE`
- Timestamps: `TIMESTAMPTZ` — nunca `TIMESTAMP` sem timezone
- IDs: `BIGSERIAL` (autoincremento)
- Strings: `VARCHAR(n)` com tamanho adequado
- Nomes de constraint com prefixo: `pk_`, `fk_`, `uk_`, `idx_`, `ck_`

## Entidades JPA

### Lock otimista em WalletEntity
```java
@Entity
@Table(name = "wallets")
class WalletEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Version  // obrigatório — lock otimista para concorrência em saldo
    Long version;

    BigDecimal balance;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    UserEntity user;
}
```

- `@Version` é obrigatório em `WalletEntity` — sem ele não há proteção contra race condition de saldo.
- `FetchType.LAZY` em todas as associações — carregue eager apenas quando explicitamente necessário.
- Entidades ficam em `.internal` — nunca no pacote público do módulo.

### Regras de mapeamento

- Nunca retorne `*Entity` diretamente na API — mapeie para record/DTO no boundary do módulo.
- Não use `@OneToMany` sem `cascade` e `orphanRemoval` bem pensados.
- Use `@Column(nullable = false)` em todos os campos obrigatórios — alinhado com constraint NOT NULL no SQL.

## Outbox transacional

A tabela `event_publication` é gerenciada automaticamente pelo `spring-modulith-starter-jpa`. O DDL deve ser criado manualmente em `V1__create_event_publication.sql` porque com Flyway o Hibernate não gera o schema automaticamente.

Configure em `application.properties`:
```properties
spring.threads.virtual.enabled=true
spring.modulith.events.republish-outstanding-events-on-restart=true
spring.datasource.url=jdbc:postgresql://localhost:5432/walletflow
spring.datasource.username=postgres
spring.datasource.password=postgres
spring.jpa.hibernate.ddl-auto=validate
```

`ddl-auto=validate` garante que o schema do banco bate com as entidades JPA — sem geração automática, Flyway é a fonte da verdade.

## Módulos relacionados

- `java-core.md` — estrutura de módulos e entidades
- `java-testing.md` — H2 nos testes e ApplicationModuleTest
- `security.md` — senhas e dados sensíveis
