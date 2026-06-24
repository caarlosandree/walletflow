Execute a próxima task pendente do `implementation-plan.md` seguindo o workflow TDD do Walletflow.

## Fluxo

1. Leia `implementation-plan.md` e identifique a primeira task `[ ]` não concluída.
2. Leia `docs/design/architecture.md` para entender o design da task.
3. Siga o ciclo TDD:
   - Escreva o teste antes da implementação
   - Implemente o mínimo para o teste passar
   - Refatore se necessário
4. Execute `./gradlew test` — deve estar verde.
5. Execute `./gradlew build` — deve estar verde (inclui `ModularityTests.verify()`).
6. Marque a task como `[x]` no `implementation-plan.md`.
7. Faça commit: `<tipo>: <descrição em pt-BR>` (máx 72 chars, sem Co-authored-by).

## Restrições

- Nunca edite migration Flyway existente.
- Nunca adicione `spring-boot-starter-security`.
- Chamadas HTTP externas sempre fora de `@Transactional`.
- DTOs como records com validações Jakarta.
- Injeção por construtor (`@RequiredArgsConstructor`).
- Nenhuma `*Entity` exposta fora de `.internal`.
