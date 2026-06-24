---
trigger: always_on
description: Regras de segurança relevantes para o Walletflow (backend sem autenticação)
---

# Walletflow — Segurança

## Escopo

Este projeto **não tem autenticação/JWT** (decisão D3 do design). Não adicione `spring-boot-starter-security` — isso ativaria a filter chain e bloquearia todos os endpoints. Use **somente** `spring-security-crypto` para BCrypt.

## Senhas

```java
// CORRETO — apenas o módulo de crypto
implementation 'org.springframework.security:spring-security-crypto'

// PROIBIDO — ativa filter chain e bloqueia endpoints
// implementation 'org.springframework.boot:spring-boot-starter-security'
```

Sempre armazene senha com BCrypt:
```java
private final PasswordEncoder encoder = new BCryptPasswordEncoder();

String hashed = encoder.encode(rawPassword);
```

Nunca armazene ou logue a senha em texto plano.

## Validação de entrada

Toda entrada do usuário é validada via Jakarta Bean Validation antes de chegar ao service:

```java
public record CreateUserRequest(
    @NotBlank String fullName,
    @NotBlank @Size(min = 11, max = 14) String document,
    @Email @NotBlank String email,
    @NotBlank @Size(min = 6) String password,
    @NotNull UserType type
) {}
```

- Nunca confie em dados vindos do corpo da requisição sem validação.
- Use `@Valid` no parâmetro do controller.
- `Document.of()` valida CPF/CNPJ com verificação de dígito verificador — não apenas o tamanho.

## SQL e JPA

- Use Spring Data JPA e JPQL com parâmetros nomeados — nunca concatenação de strings em queries.
- Nunca construa queries com entrada do usuário diretamente.
- Migrations Flyway são arquivos `.sql` estáticos — não gere SQL dinamicamente.

## Logs seguros

- Nunca logue senha, CPF completo, CNPJ completo ou email em texto plano em produção.
- Excepções de domínio podem ter mensagem descritiva, mas nunca com dados sensíveis do usuário.
- Stack traces não devem aparecer na resposta da API — use `GlobalExceptionHandler` para garantir isso.

## Secrets e configuração

- Credenciais de banco (datasource URL, user, password) em variáveis de ambiente ou `application.properties` local — nunca commitadas.
- `.env` e arquivos com senhas devem estar no `.gitignore`.
- Se precisar de um `.env.example`, use valores fictícios.

## Chamadas HTTP externas

- `AuthorizationClient` e `NotifyClient` usam `RestClient` com timeouts explícitos — sem timeout indefinido.
- Não reutilize conexões HTTP em contexto transacional.
- Falha de rede no autorizador deve lançar exceção que reverte a transferência — não silencie erros de I/O.

## Idempotência

- `IdempotencyRecord` previne transferência duplicada via constraint `UNIQUE(idempotency_key)` no Postgres.
- Não use Redis para isso — a constraint do banco é suficiente (decisão D7).

## Módulos relacionados

- `java-api.md` — validação de entrada nos controllers
- `database.md` — segurança em queries e migrations
