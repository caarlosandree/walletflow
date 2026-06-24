Verifique se a task atual do Walletflow está realmente concluída antes de fazer commit.

## Checklist de verificação

### Build
- Execute `./gradlew build` e confirme que está verde.
- `ModularityTests.verify()` deve passar (incluso no build).

### Fronteiras de módulo
- Nenhuma `*Entity` exposta fora de `.internal`.
- Nenhum import de `{modulo}.internal` de outro módulo.

### Regras de negócio
- `Money.debit()` rejeita resultado negativo.
- Lojista (`Merchant`) não consegue iniciar transferência.
- Autorizador consultado fora de `@Transactional`.
- `TransferCompleted` publicado dentro da TX.

### Banco
- Nova migration criada com nome correto (`V{n}__{descricao}.sql`).
- Migration existente não foi editada.
- Colunas monetárias usam `DECIMAL`, não `FLOAT`.

### Testes
- Pelo menos um teste para o caminho feliz e um para erro.
- Nenhum `Thread.sleep` — usar `Scenario` para eventos.
- `@ApplicationModuleTest` cobre o módulo da task.

### Segurança
- Nenhum segredo no código.
- Nenhuma senha logada em texto plano.

### Commit
- Mensagem segue `<tipo>: <descrição em pt-BR>` (máx 72 chars).
- Sem `Co-authored-by`.

Reporte o que passou e o que ainda está pendente.
