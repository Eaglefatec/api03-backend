# 🌿 Estratégia de Branches

O projeto utiliza uma estratégia de versionamento baseada no **Git Flow**, adaptada para o fluxo de desenvolvimento por **Sprints, User Stories e Tasks**.

A estrutura tem como objetivo manter a branch principal estável, organizar o desenvolvimento de cada Sprint e permitir que as funcionalidades sejam desenvolvidas de forma isolada por meio de branches menores.

---

## 📌 Estrutura das Branches

A estratégia de branches segue a seguinte hierarquia:

```text
main
 ├── sprint-1
 │    ├── task-01
 │    ├── task-02
 │    └── task-03
 │
 ├── sprint-2
 │    ├── task-01
 │    ├── task-02
 │    └── task-03
 │
 └── sprint-3
      ├── task-01
      ├── task-02
      └── task-03
```

### Branch `main`

A branch `main` representa a **versão principal e estável do projeto**.

* É a branch de referência do projeto.
* Deve conter apenas código considerado estável.
* Novas Sprints devem sempre ser criadas a partir da `main`.
* Alterações não devem ser realizadas diretamente na `main`.
* A integração de alterações deve ocorrer por meio de **Pull Requests**.

```text
main
```

---

### 🏃 Branches de Sprint

Para cada Sprint, deve ser criada uma branch específica diretamente a partir da `main`.

O padrão de nomenclatura é:

```text
sprint-X
```

Onde `X` representa o número da Sprint.

### Exemplos

```text
sprint-1
sprint-2
sprint-3
```

Cada branch de Sprint concentra o desenvolvimento realizado durante aquele ciclo.

```text
main
 ├── sprint-1
 ├── sprint-2
 └── sprint-3
```

> **Importante:** toda branch de Sprint deve ser criada a partir da versão atualizada da `main`.

---

## 🔧 Branches de Tasks

As atividades menores de desenvolvimento devem ser realizadas em branches próprias, criadas a partir da branch da Sprint correspondente.

O objetivo é manter cada alteração isolada, facilitando o desenvolvimento, revisão e controle das mudanças.

### Estrutura

```text
sprint-X
 ├── task-01
 ├── task-02
 └── task-03
```

### Exemplos

```text
sprint-1
 ├── task-01
 ├── task-02
 └── task-03
```

Ou, caso seja necessário identificar a User Story:

```text
US01-01-task-01
US01-01-task-02
US01-02-task-01
```

A nomenclatura das branches de Task deve ser definida de forma consistente pela equipe.

---

## 🔄 Fluxo de Desenvolvimento

O fluxo padrão para desenvolvimento de uma nova funcionalidade deve seguir as seguintes etapas:

```text
                 ┌─────────────┐
                 │    main     │
                 └──────┬──────┘
                        │
                        ▼
                 ┌─────────────┐
                 │   sprint-1  │
                 └──────┬──────┘
                        │
              ┌─────────┼─────────┐
              ▼         ▼         ▼
          task-01   task-02   task-03
              │         │         │
              └─────────┼─────────┘
                        │
                        ▼
                 Pull Request
                        │
                        ▼
                    sprint-1
                        │
                        ▼
                 Pull Request
                        │
                        ▼
                      main
```

### 1. Criar a Sprint

A branch da Sprint deve ser criada a partir da `main`:

```bash
git checkout main
git pull origin main
git checkout -b sprint-1
git push -u origin sprint-1
```

### 2. Criar uma Task

A branch da Task deve ser criada a partir da Sprint correspondente:

```bash
git checkout sprint-1
git pull origin sprint-1
git checkout -b task-01
git push -u origin task-01
```

### 3. Desenvolver a Task

Os commits devem seguir o **[Padrão de Commits](#-padrão-de-commits)** definido pelo projeto.

Exemplo:

```bash
git commit -m "feat(US01-01): Implementado endpoint de cadastro" -m "Adicionado endpoint responsável pelo cadastro de usuários."
```

### 4. Abrir Pull Request

Após finalizar a Task, deve ser aberto um **Pull Request** da branch da Task para a branch da Sprint:

```text
task-01 → sprint-1
```

O Pull Request deve ser revisado antes da integração.

### 5. Finalizar a Sprint

Ao final da Sprint, após todas as Tasks terem sido concluídas, revisadas e integradas, deve ser aberto um Pull Request da branch da Sprint para a `main`:

```text
sprint-1 → main
```

Após a aprovação, a Sprint pode ser integrada à `main`.

---

## 🔀 Pull Requests

Os Pull Requests são obrigatórios para a integração de código entre branches.

### Fluxo dos Pull Requests

```text
Task → Sprint → Main
```

Exemplo:

```text
task-01 → sprint-1 → main
```

### Regras

* Não realizar commits diretamente na `main`.
* Não realizar commits diretamente na branch de Sprint quando a alteração puder ser desenvolvida em uma Task.
* Toda Task deve passar por Pull Request antes de ser integrada à Sprint.
* A integração da Sprint com a `main` deve ocorrer por meio de Pull Request.
* O código deve ser revisado antes do merge.
* O Pull Request deve possuir uma descrição clara sobre as alterações realizadas.
* O responsável pela implementação não deve ser o único responsável pela aprovação do Pull Request.
* Conflitos devem ser resolvidos antes da aprovação e integração.

---

## 🏷️ Padrão de Nomenclatura

As branches devem utilizar nomes curtos, objetivos e padronizados.

### Sprint

```text
sprint-X
```

Exemplos:

```text
sprint-1
sprint-2
sprint-3
```

### Task

```text
task-descricao
```

Exemplos:

```text
task-login
task-cadastro-usuario
task-endpoint-metricas
```

### Task vinculada à User Story

Quando necessário, pode-se utilizar:

```text
USXX-XX-task-descricao
```

Exemplos:

```text
US01-01-task-login
US01-02-task-cadastro
US02-01-task-dashboard
```

---

## ⚠️ Regras Importantes

1. A `main` deve permanecer estável.
2. Branches de Sprint devem sempre partir da `main`.
3. Branches de Task devem partir da Sprint correspondente.
4. Não desenvolver diretamente na `main`.
5. Evitar desenvolver diretamente na branch de Sprint.
6. Toda alteração deve estar associada a uma Task ou User Story quando aplicável.
7. Toda Task deve ser integrada à Sprint por meio de Pull Request.
8. A Sprint deve ser integrada à `main` por meio de Pull Request.
9. Pull Requests devem ser revisados e aprovados antes do merge.
10. Commits devem seguir o padrão definido na documentação do projeto.

---

## 📊 Resumo do Fluxo

| Branch       | Origem     | Finalidade                            | Destino    |
| ------------ | ---------- | ------------------------------------- | ---------- |
| **main**     | —          | Versão principal e estável do projeto | —          |
| **sprint-X** | `main`     | Desenvolvimento de uma Sprint         | `main`     |
| **task-***   | `sprint-X` | Desenvolvimento de uma Task           | `sprint-X` |

### Fluxo final

```text
main
  │
  ├── sprint-1
  │      ├── task-01 ──┐
  │      ├── task-02 ──┼──→ Pull Request → sprint-1
  │      └── task-03 ──┘
  │
  └── Pull Request → main
```

> **Regra principal:** `main` → `sprint-X` → `task-*` → Pull Request → `sprint-X` → Pull Request → `main`.
