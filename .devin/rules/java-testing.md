---
trigger: always_on
description: Estratégia de testes do Walletflow com JUnit 5 e Spring Modulith Test
---

# Walletflow — Testes

## Regra fundamental: TDD

Cada task do `implementation-plan.md` segue TDD: **teste primeiro, implementação depois**. Execute `./gradlew test` ao final de cada task. O `build` só fecha verde quando todos os testes passam.

## Tipos de teste e ferramentas

| Tipo | Ferramenta | Escopo |
|---|---|---|
| Unitário | JUnit 5 + AssertJ | Value objects, lógica pura (`Money`, `Document`) |
| Módulo isolado | `@ApplicationModuleTest` | Cada módulo sobe com mocks dos vizinhos |
| Verificação arquitetural | `ApplicationModules.verify()` | Ciclos, acessos a `.internal`, dependências não declaradas |
| Evento assíncrono | `Scenario` / `PublishedEvents` | `transfer → TransferCompleted → notification` |

**Banco nos testes:** H2 em memória — sem Testcontainers, sem Docker no ciclo de teste (decisão D6).

## Verificação arquitetural (obrigatória, T6)

```java
class ModularityTests {
    static final ApplicationModules modules =
        ApplicationModules.of(WalletflowApplication.class);

    @Test void semCiclosNemAcessoIlegal() {
        modules.verify();
    }

    @Test void documentacao() {
        new Documenter(modules)
            .writeDocumentation()
            .writeIndividualModulesAsPlantUml();
    }
}
```

`modules.verify()` quebra o `test` (e o `build`) se houver ciclo, acesso a `.internal` de outro módulo ou dependência não declarada. Deve rodar sem erro antes de qualquer PR.

## Módulo isolado com `@ApplicationModuleTest`

```java
@ApplicationModuleTest
class UserModuleTest {

    @Autowired
    UserDirectory userDirectory;

    @Test
    void deveRecusarEmailDuplicado() {
        var request = new CreateUserRequest("Ana", "12345678901", "ana@test.com", "senha", COMMON);
        userDirectory.create(request);

        assertThatThrownBy(() -> userDirectory.create(request))
            .isInstanceOf(DuplicateUserException.class);
    }
}
```

O `@ApplicationModuleTest` sobe apenas o módulo `user` com H2, sem precisar dos outros módulos.

## Teste de evento assíncrono com `Scenario`

```java
@ApplicationModuleTest
class TransferEventTests {

    @Test
    void publicaTransferCompletedAposSucesso(Scenario scenario) {
        scenario.stimulate(() -> transferService.transfer(new TransferCommand(payerId, payeeId, Money.of("100"))))
                .andWaitForEventOfType(TransferCompleted.class)
                .toArriveAndVerify(evt -> {
                    assertThat(evt.payerId()).isEqualTo(payerId);
                    assertThat(evt.amount()).isEqualTo(Money.of("100"));
                });
    }
}
```

`Scenario` elimina flakiness de testes assíncronos — aguarda o evento chegar sem `Thread.sleep`.

## Testes unitários de value objects

```java
class MoneyTest {

    @Test void deveRejeitarDebitoQueGerarSaldoNegativo() {
        var saldo = new Money(new BigDecimal("50.00"));
        var debito = new Money(new BigDecimal("60.00"));

        assertThatThrownBy(() -> saldo.debit(debito))
            .isInstanceOf(InsufficientBalanceException.class);
    }

    @Test void deveAceitarDebitoExatoDoSaldo() {
        var saldo = new Money(new BigDecimal("50.00"));
        var resultado = saldo.debit(new Money(new BigDecimal("50.00")));
        assertThat(resultado.amount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
```

## Padrão AAA

Todos os testes seguem Arrange / Act / Assert:

```java
@Test void descricaoDoComportamentoEsperado() {
    // Arrange
    var ...

    // Act
    var resultado = ...

    // Assert
    assertThat(resultado)...
}
```

Nomes de testes em pt-BR, descrevendo o comportamento esperado (`deveRejeitarLojistaPagador`, `devePublicarEventoAposTransferencia`).

## Antipadrões proibidos

- Teste sem asserção significativa.
- Testes que dependem de ordem de execução ou estado compartilhado.
- `Thread.sleep` em testes assíncronos — use `Scenario`.
- Testar comportamento do framework (não teste que `@NotBlank` funciona — isso é responsabilidade do Bean Validation).
- Editar migrations Flyway existentes para fazer testes passarem.

## Módulos relacionados

- `java-core.md` — estrutura de módulos testada
- `java-checklist.md` — critérios de conclusão de task
- `database.md` — migrations e H2
