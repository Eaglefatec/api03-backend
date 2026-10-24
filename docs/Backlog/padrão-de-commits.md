# 📋 Padrão de Commits

Os commits do projeto devem seguir o seguinte padrão:

```text
Tipo de commit (User Story - Task): Título para alteração

Descrição com detalhamento sobre o que foi modificado e para qual finalidade.
```

### 🛠️ Estrutura

```bash
git commit -m "tipo(USXX-XX): Título da alteração" -m "Descrição detalhada da alteração e sua finalidade."
```

### 💻 Exemplo

```bash
git commit -m "feat(US01-01): Implementado conexão com o Banco de Dados" -m "Incremento de um micro-serviço 'MongoDBConnectionService' para conectar o MongoDB ao backend da aplicação, permitindo operações de leitura e escrita de forma padronizada e centralizada."
```

---

## ⚠️ Exceções

Quando a modificação não estiver diretamente relacionada ao código ou à funcionalidade do projeto, mas sim à **organização do repositório ou documentação**, não é necessário informar a User Story e a Task.

### Exemplos

```text
docs: Adicionado diagrama de banco de dados
```

```text
chore: Reorganizado arquivos e ajustado dependências
```

---

## 🏷️ Tipos de Commit

| Tipo         | Descrição                                                                     | Exemplo                                                                            |
| ------------ | ----------------------------------------------------------------------------- | ---------------------------------------------------------------------------------- |
| **chore**    | Atualizações menores que não impactam diretamente a funcionalidade do código. | `chore: Reorganizado arquivos e ajustado dependências`                             |
| **docs**     | Atualizações ou adições na documentação.                                      | `docs: Adicionado diagrama de banco de dados`                                      |
| **feat**     | Adição de um novo recurso ou funcionalidade.                                  | `feat(US01-01): Implementado filtro de visualização semanal e mensal das métricas` |
| **fix**      | Correção de bugs ou falhas identificadas.                                     | `fix(US01-01): Corrigido erro no endpoint de login`                                |
| **refactor** | Refatoração do código sem alteração de seu comportamento.                     | `refactor(US01-01): Refatorada estrutura de pastas do back-end`                    |
| **style**    | Mudanças de formatação e estilo, sem alteração da lógica do código.           | `style(US01-01): Ajustados nomes de variáveis para o padrão camelCase`             |
| **test**     | Criação ou modificação de testes automatizados.                               | `test(US01-01): Adicionados testes para endpoint de login`                         |

---

## 📚 Resumo

Para alterações relacionadas ao desenvolvimento:

```text
tipo(User Story - Task): Título da alteração
```

Para alterações relacionadas à organização ou documentação:

```text
tipo: Título da alteração
```

### Principais tipos

* `feat` → Nova funcionalidade
* `fix` → Correção de bug
* `refactor` → Refatoração
* `style` → Formatação/estilo
* `test` → Testes
* `docs` → Documentação
* `chore` → Manutenção e organização
