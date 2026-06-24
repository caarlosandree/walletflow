---
trigger: model_decision
description: Checklist que o agente executa antes de encerrar qualquer task do Walletflow
---

# Walletflow — Checklist pré-conclusão de task

Execute este checklist antes de declarar qualquer task do `implementation-plan.md` como concluída.

## Build e compilação

- [ ] `./gradlew build` passa sem erros de compilação
- [ ] Nenhum `@SuppressWarnings` adicionado para silenciar erro real

## Fronteiras de módulo (Spring Modulith)

- [ ] `./gradlew test` passa — `ModularityTests.verify()` incluso
- [ ] Nenhuma entidade `*Entity` exposta para outro módulo
- [ ] Nenhum acesso a `.internal` de outro módulo
- [ ] Se adicionou dependência entre módulos, atualizou `architecture.md`

## Regras de negócio

- [ ] `Money.debit()` rejeita resultado negativo
- [ ] `Document.of()` valida CPF (11 dígitos + DV) e CNPJ (14 dígitos + DV)
- [ ] Lojista (`Merchant`) não consegue iniciar transferência
- [ ] Autorizador externo consultado **fora** de `@Transactional`
- [ ] Evento `TransferCompleted` publicado **dentro** da TX de transferência

## Transações

- [ ] Nenhuma chamada HTTP externa dentro de bloco `@Transactional`
- [ ] `@Transactional` aparece apenas em services (`*ServiceImpl`), nunca em controllers
- [ ] `WalletEntity` tem `@Version` para lock otimista

## Banco e migrations

- [ ] Nova migration Flyway criada com nome `V{n}__{descricao}.sql`
- [ ] Migration existente **não foi editada**
- [ ] Colunas monetárias usam `DECIMAL`, nunca `FLOAT` ou `DOUBLE`
- [ ] Chaves estrangeiras têm índice correspondente

## Testes

- [ ] Teste escrito antes da implementação (TDD)
- [ ] Caso feliz e pelo menos um caso de erro cobertos
- [ ] Nenhum `Thread.sleep` nos testes — usar `Scenario` para eventos async
- [ ] `@ApplicationModuleTest` cobre o módulo implementado nesta task

## Código

- [ ] DTOs são records (imutáveis)
- [ ] Injeção por construtor (`@RequiredArgsConstructor`)
- [ ] Nenhuma entidade JPA retornada diretamente no controller
- [ ] Exceções de domínio tratadas no `GlobalExceptionHandler`
- [ ] Nenhum segredo ou credencial no código

## Commit

- [ ] Mensagem segue `<tipo>: <descrição em pt-BR>` (máx 72 chars)
- [ ] Um commit por mudança lógica
- [ ] Sem `Co-authored-by` na mensagem
