---
trigger: always_on
description: Contratos REST, DTOs, exceções e tratamento de erros do Walletflow
---

# Walletflow — API REST

## Endpoints

| Método | Path | Status sucesso | Corpo |
|---|---|---|---|
| POST | `/users` | 201 | `UserResponse` |
| POST | `/transfer` | 200 | resultado da transferência |

Não há versionamento de path (`/api/v1`) neste projeto — as rotas são diretas e o escopo é fixo.

## Controllers — regra de ouro

Controllers são finos. Fazem apenas duas coisas: validar entrada e delegar ao service.

```java
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserDirectory userDirectory;

    @PostMapping
    public ResponseEntity<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        var user = userDirectory.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
}
```

Nunca coloque lógica de negócio no controller.

## DTOs como records

```java
public record CreateUserRequest(
    @NotBlank String fullName,
    @NotBlank String document,
    @Email @NotBlank String email,
    @NotBlank String password,
    @NotNull UserType type
) {}

public record UserResponse(Long id, String fullName, String email, UserType type) {}
```

- Nunca retorne `*Entity` diretamente na resposta.
- Records são imutáveis — preferidos para request e response.
- Validações Jakarta (`@NotBlank`, `@Email`, `@NotNull`) ficam no record de request.
- Ative com `@Valid` no parâmetro do controller.

## Tratamento de erros

Implemente um `@RestControllerAdvice` global. Mapeie exceções de domínio para status HTTP corretos.

```java
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MerchantCannotSendException.class)
    public ResponseEntity<ErrorResponse> handleMerchantCannotSend(MerchantCannotSendException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ErrorResponse("MERCHANT_CANNOT_SEND", ex.getMessage()));
    }

    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ErrorResponse> handleInsufficientBalance(InsufficientBalanceException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ErrorResponse("INSUFFICIENT_BALANCE", ex.getMessage()));
    }

    @ExceptionHandler(TransferDeniedException.class)
    public ResponseEntity<ErrorResponse> handleTransferDenied(TransferDeniedException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
            .body(new ErrorResponse("TRANSFER_DENIED", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        // ... extrair field errors
        return ResponseEntity.badRequest().body(...);
    }
}
```

## Status HTTP de referência

| Situação | Status |
|---|---|
| Usuário criado com sucesso | 201 Created |
| Transferência executada | 200 OK |
| Dado duplicado (CPF/CNPJ/Email já existe) | 409 Conflict |
| Validação de entrada falhou | 400 Bad Request |
| Lojista tentou enviar / saldo insuficiente / autorizador recusou | 422 Unprocessable Entity |
| Recurso não encontrado | 404 Not Found |

## Clientes HTTP declarativos (`@HttpExchange`)

```java
@HttpExchange
public interface AuthorizationClient {
    @GetExchange("https://util.devi.tools/api/v2/authorize")
    AuthorizationResponse authorize();
}
```

Configure o bean via `HttpServiceProxyFactory` + `RestClient` com timeouts explícitos. Chame sempre **fora** de `@Transactional`.

## Módulos relacionados

- `java-core.md` — arquitetura de módulos
- `java-testing.md` — testes de controller e integração
- `security.md` — validação de entrada e tratamento de erros seguros
