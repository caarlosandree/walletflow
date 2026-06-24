---
trigger: model_decision
description: Padrão de commit convencional em português brasileiro para o Walletflow
---

# Walletflow — Commits

## Formato

```
<tipo>: <descrição>

[corpo opcional — por que, não o quê]
```

## Tipos permitidos

| Tipo | Quando usar |
|---|---|
| `feat` | Nova funcionalidade de domínio (novo módulo, endpoint, regra de negócio) |
| `fix` | Correção de bug |
| `test` | Adiciona ou corrige testes |
| `refactor` | Melhoria interna sem mudança de comportamento |
| `chore` | Build, dependências, config (sem código de produção) |
| `docs` | Documentação, architecture.md, implementation-plan.md |
| `perf` | Otimização de performance |
| `ci` | Pipelines CI/CD |

## Regras da descrição

- Máximo **72 caracteres**
- Modo imperativo em pt-BR: `adiciona`, `corrige`, `remove`, `extrai`, `implementa`
- Primeira letra **minúscula**
- Sem ponto final
- Sem `Co-authored-by` ou `Co-Authored-By`

## Exemplos corretos

```
feat: implementa módulo shared com Money e Document
test: adiciona testes unitários de CPF e CNPJ no Document
feat: adiciona UserController e cadastro de usuário
fix: corrige validação de DV do CNPJ
chore: adiciona migration V1 da tabela event_publication
refactor: extrai lógica de autorização para AuthorizationClient
test: valida publicação de TransferCompleted com Scenario
docs: atualiza implementation-plan com tasks T0-T6
```

## Escopo de um commit

Um commit = uma mudança lógica. Ao final de cada task do `implementation-plan.md`, faça um commit descrevendo a task concluída. Não agrupe tasks não relacionadas em um único commit.
