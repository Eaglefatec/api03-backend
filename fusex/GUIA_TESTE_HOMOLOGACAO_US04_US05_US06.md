# Guia Passo a Passo de Teste e Homologação — Assinatura e Envio de Guias (US04, US05 e US06)

Este guia foi escrito de forma **simples, didática e passo a passo** para que qualquer pessoa ou desenvolvedor iniciante consiga rodar a aplicação, abrir as telas no navegador e testar todo o funcionamento do **Front-end**, do **Back-end**, do **Banco de Dados** e da **gravação dos PDFs assinados**, desde o envio inicial do PDF até o disparo do e-mail ao beneficiário.

---

## 1. Como funciona esta tela no projeto atual?

Para evitar conflitos de código (`git merge`) com o desenvolvedor que está refazendo a tela de login e a tela inicial do Emissor de Guias, todo o módulo das **US04**, **US05** e **US06** foi construído na página:

👉 **`assinatura-guias.html`**

- **Já possui o menu na barra lateral esquerda** seguindo o padrão visual oficial do sistema.
- **Possui um seletor "Simular login" no canto superior direito**, que troca automaticamente o usuário autenticado no Back-end entre:
  - **Emissor de Guias** (usuário `eg` / senha `senha123`)
  - **Médico Chefe FUSEX** (usuário `medicochefe` / senha `senha123`)
  - **Chefe da Seção FUSEX** (usuário `chefesecao` / senha `senha123`)
- **Já vem com 5 guias de exemplo pré-carregadas** (`202610700` a `202610704`) assim que o servidor liga, para que você já veja dados reais nas tabelas imediatamente.

---

## 2. Como Ligar a Aplicação (Passo 0)

O projeto suporta **dois modos** para ligar o banco de dados:
- **Modo H2 em memória (Recomendado para teste rápido — não precisa instalar MySQL nem Docker!)**
- **Modo Padrão com MySQL via Docker** (caso você já esteja com o container MySQL do projeto rodando).

Escolha **uma** das opções abaixo:

### Opção A — Rodar SEM MySQL / SEM Docker (Banco H2 em memória — Mais rápido!)
1. Abra o terminal (PowerShell ou Prompt de Comando) na pasta `fusex`:
   ```powershell
   cd fusex
   ```
2. Execute o comando abaixo ativando o perfil `h2`:
   ```powershell
   mvn spring-boot:run "-Dspring-boot.run.profiles=h2"
   ```
   *(Se preferir rodar pela IDE — VS Code / IntelliJ / Eclipse — basta definir a variável de ambiente `SPRING_PROFILES_ACTIVE=h2` na configuração de execução do `FusexApplication.java` e clicar em **Run ▶**).*
3. Aguarde aparecer no terminal a mensagem:
   `Started FusexApplication in ... seconds` *(Deixe essa janela do terminal aberta enquanto estiver testando).*

### Opção B — Rodar COM MySQL via Docker (Perfil padrão `local` do projeto)
1. Caso tenha o **Docker Desktop** aberto, suba o banco MySQL na pasta `fusex`:
   ```powershell
   cd fusex
   docker compose up -d
   ```
2. Depois, clique em **Run ▶** no arquivo `src/main/java/com/eagle/fusex/FusexApplication.java` na sua IDE, ou rode no terminal:
   ```powershell
   mvn spring-boot:run
   ```
3. Quando aparecer `Started FusexApplication`, o sistema está no ar!

---

## 3. Como Acessar a Tela no Navegador

Com a aplicação ligada, abra o seu navegador (Chrome, Edge ou Firefox) e digite o endereço abaixo:

🔗 **`http://localhost:8080/api/assinatura-guias.html`**

> **Nota:** Se o navegador abrir uma janelinha pedindo usuário e senha na primeira requisição, digite:
> - **Usuário:** `eg`
> - **Senha:** `senha123`

Você também pode acessar diretamente cada perfil através de links diretos (deep-links):
- **Visão do Emissor (Upload):** `http://localhost:8080/api/assinatura-guias.html?role=emitter&view=upload`
- **Visão do Emissor (Envio por E-mail):** `http://localhost:8080/api/assinatura-guias.html?role=emitter&view=email`
- **Visão do Médico Chefe:** `http://localhost:8080/api/assinatura-guias.html?role=med`
- **Visão do Chefe da Seção:** `http://localhost:8080/api/assinatura-guias.html?role=sec`

---

## 4. Roteiro Completo de Teste (Do Upload até o E-mail ao Beneficiário)

Siga os **4 passos abaixo** na ordem para testar uma guia passando por todas as etapas da esteira:

---

### PASSO 1 — Fazer o Upload da Guia como Emissor de Guias (US04)

1. No canto superior direito da tela, confirme que o campo **"Simular login"** está selecionado como **`Emissor de Guias`**.
2. Observe o menu na barra lateral esquerda: você estará em **`⇧ Envio de guias`**.
3. Role até a tabela **"Acompanhamento das guias"**: você já verá 5 guias demonstrativas (`202610700` a `202610704`), cada uma com sua tag colorida de status.
4. **Onde pegar um arquivo PDF para testar o upload?**
   - Assim que a aplicação liga, ela já gera um PDF de exemplo inédito pronto para upload em **`fusex/uploads/exemplo_nova_guia_202610710.pdf`** (com Nº da Guia `202610710`, Beneficiário, Prec-CP e OCS já formatados para você ver a extração automática em ação!).
   - *(Atenção: não escolha `guia_202610700.pdf` a `guia_202610704.pdf`, pois essas 5 guias já estão cadastradas na tabela inicial e o sistema bloqueará corretamente avisando que a guia já existe! Você também pode enviar **qualquer outro arquivo `.pdf` válido do seu computador** com menos de **5 MB** — caso ele não tenha número de guia no texto, o sistema gerará automaticamente o próximo número sequencial, como `202610705`).*
5. **Teste de validação de erro (opcional):**
   - Tente selecionar um arquivo `.txt` ou `.png`, ou um PDF maior que 5 MB. O sistema bloqueará o envio mostrando uma mensagem de erro vermelha.
6. **Fazendo o envio real:**
   - Clique na área pontilhada **"Arraste o PDF da guia para cá"** (ou arraste um PDF para cima dela).
   - O nome do arquivo e o tamanho em KB vão aparecer logo abaixo, e o botão verde **"Enviar guia ao sistema"** ficará habilitado.
   - Clique em **"Enviar guia ao sistema"**.
7. **O que você deve conferir (Resultado esperado):**
   - Aparecerá um quadro verde escrito **"✓ PDF processado e guia registrada"** mostrando o **Nº da guia**, **Beneficiário** e **OCS** identificados automaticamente sem você ter digitado nada.
   - Na tabela **"Acompanhamento das guias"** logo abaixo, a sua nova guia aparecerá no topo da lista com a tag amarela:
     🟡 **`Aguardando Médico Chefe`**

---

### PASSO 2 — Conferir e Assinar como Médico Chefe (US05 — 1ª Assinatura)

1. No canto superior direito da tela, mude o seletor **"Simular login"** para **`Médico Chefe FUSEX`**.
2. Observe que a barra lateral esquerda mudou para o ambiente do **Médico Chefe**, mostrando o botão **`✍ Painel de assinaturas`** com um contador numérico das guias pendentes.
3. Na tabela **"Guias aguardando assinatura do Médico Chefe"**, você verá **apenas** as guias que estão com a tag 🟡 **`Aguardando Médico Chefe`** (incluindo a guia que você acabou de enviar no Passo 1!).
4. Na linha da guia que você enviou, clique no botão **"Abrir guia"**.
5. **O que vai abrir na tela (Box de Exibição em 2 colunas):**
   - **No lado esquerdo:** O espelho oficial da guia em formato de documento militar (Ministério da Defesa / Exército Brasileiro), mostrando o Nº da Guia, Beneficiário, Prec CP, Prestador (OCS), a tabela de Procedimentos, a regra de validade e os dois espaços em branco para assinatura no rodapé (`Médico Chefe FUSEX` e `Chefe da Seção FUSEX`).
   - *(Você também pode clicar no botão **"Ver PDF original"** no topo direito do card para abrir o arquivo PDF real em uma nova aba).*
   - **No lado direito:** O painel **"Conferência e assinatura"**, mostrando o resumo dos dados, a informação **`Validade: 60 dias a partir da assinatura do Chefe da Seção`**, a caixinha de seleção (checkbox) e o quadro branco para desenhar a assinatura.
6. **Teste a trava de segurança:**
   - Veja que o botão verde **"Confirmar assinatura"** começa **desabilitado (bloqueado)**.
   - Marque apenas a caixinha *"Confirmo que conferi os dados da guia e autorizo a assinatura"* sem desenhar nada: o botão **continua bloqueado**.
7. **Desenhe a assinatura:**
   - Com o mouse (ou dedo/touchpad), desenhe uma rubrica dentro do quadro branco **"Desenhe sua assinatura no campo abaixo"**.
   - *(Se quiser testar, clique em **"Limpar assinatura"** — o quadro fica limpo e o botão verde volta a bloquear. Desenhe novamente).*
   - Agora que a caixinha está marcada **E** há uma rubrica desenhada, o botão verde **"Confirmar assinatura"** ficará **habilitado**.
8. Clique em **"Confirmar assinatura"**.
9. **O que você deve conferir (Resultado esperado):**
   - Aparecerá uma notificação no canto inferior direito: *"Assinatura registrada com sucesso."*
   - A guia **sai** da fila de pendências do Médico Chefe (pois ele já cumpriu a parte dele).
   - *(Se você voltar rapidamente o seletor "Simular login" para `Emissor de Guias`, verá na tabela "Acompanhamento das guias" que a coluna "Médico Chefe" agora tem a data/hora da assinatura e a tag mudou de amarela para azul:* 🔵 **`Aguardando Chefe da Seção`**).*

---

### PASSO 3 — Conferir e Assinar como Chefe da Seção (US05 — 2ª Assinatura e Validade de 60 Dias)

1. No canto superior direito, mude o seletor **"Simular login"** para **`Chefe da Seção FUSEX`**.
2. Na tabela **"Guias aguardando assinatura do Chefe da Seção"**, aparecerão **somente** as guias que já foram assinadas pelo Médico Chefe e estão com a tag 🔵 **`Aguardando Chefe da Seção`** (incluindo a sua guia do Passo 2!).
3. Clique no botão **"Abrir guia"** na linha da sua guia.
4. **O que você deve observar no documento à esquerda:**
   - Olhe o rodapé do documento à esquerda: **a assinatura que você desenhou como Médico Chefe no Passo 2 já está aparecendo desenhada acima da linha `Médico Chefe FUSEX`!**
5. Agora, no painel direito (**"Conferência e assinatura"**):
   - Marque a caixinha **"Confirmo que conferi os dados da guia e autorizo a assinatura."**
   - Desenhe a segunda assinatura no quadro branco com o mouse.
   - Clique no botão verde **"Confirmar assinatura"**.
6. **O que acontece no Back-end neste exato momento:**
   - O sistema grava a 2ª assinatura, **calcula automaticamente a data de validade da guia somando 60 dias a partir de hoje**, estampa as duas assinaturas e a data de validade dentro do arquivo PDF físico em disco (`uploads/guia_assinada_...pdf`), e muda o status da guia para:
     🟢 **`Aguardando envio ao beneficiário`**

---

### PASSO 4 — Preparar e Enviar a Guia Assinada por E-mail ao Beneficiário (US06)

1. No canto superior direito, volte o seletor **"Simular login"** para **`Emissor de Guias`**.
2. Na tabela **"Acompanhamento das guias"**, veja que a sua guia agora tem data/hora nas colunas **"Médico Chefe"** e **"Chefe da Seção"** e exibe a tag verde 🟢 **`Aguardando envio ao beneficiário`**.
3. Agora, no **menu lateral esquerdo**, clique na segunda opção:
   **`✉ Envio por e-mail`**
4. Você verá duas tabelas:
   - **Tabela 1 — "Guias aguardando envio ao beneficiário":** Aqui está a guia que acabou de receber as duas assinaturas! Observe que a coluna **"Válida até"** já mostra a data exata daqui a **60 dias**.
   - **Tabela 2 — "Já enviadas":** Mostra o histórico de guias que já foram enviadas.
5. Na linha da sua guia (na primeira tabela), clique no botão verde **"Preparar envio"**.
6. **O que vai abrir (Modal sobreposto):**
   - Mostra o **E-mail do beneficiário (cadastro)**.
   - Mostra o quadro **"Mensagem pronta"** já preenchido com o nome do beneficiário, a explicação da validade de 60 dias, o **Nº da guia**, a **OCS** e a data **Válida até**.
   - Clique no botão **"Copiar mensagem"**: aparecerá o aviso *"Mensagem copiada."* (você pode colar em um bloco de notas com `Ctrl+V` para conferir).
   - Clique no botão **"Ver PDF assinado"**: o navegador abrirá em uma nova aba o arquivo PDF real salvo no servidor, e você verá no rodapé do PDF as **duas rubricas desenhadas**, as datas de assinatura e a frase **"Valida por 60 dias a partir da assinatura do Chefe da Secao"**.
7. Agora, de volta ao modal, clique no botão verde:
   **"Enviar por e-mail e disponibilizar na conta"**
8. **O que você deve conferir (Resultado final):**
   - O modal se fecha e aparece a confirmação: *"E-mail enviado e guia disponibilizada na conta do beneficiário."*
   - A guia **sai** da tabela *"Guias aguardando envio ao beneficiário"* e **aparece imediatamente na tabela de baixo ("Já enviadas")** com o e-mail do destinatário e a data/hora do envio!
   - E se você clicar em **`⇧ Envio de guias`** no menu lateral esquerdo, verá na tabela **"Acompanhamento das guias"** que a tag final da guia mudou para:
     ⚪ **`Enviada ao beneficiário`**

---

## 5. Como Checar que o Back-end, o Banco de Dados e os Arquivos em Disco Funcionaram?

Se você quiser comprovar tecnicamente que tudo aconteceu de verdade no Back-end (e não apenas na tela), confira estes **3 lugares**:

### A) Olhando o Log no Terminal do Java (Back-end e E-mail)
Olhe a janela do terminal onde você rodou a aplicação. Você verá uma linha de log para cada ação que fez na tela:
```text
INFO ... GuiaAssinaturaService : Guia nº 202610705 registrada na esteira de assinaturas (...)
INFO ... GuiaAssinaturaService : 1ª assinatura (Médico Chefe) registrada para a guia nº 202610705 por medicochefe
INFO ... GuiaAssinaturaService : 2ª assinatura (Chefe da Seção) registrada para a guia nº 202610705 por chefesecao. Validade fixada até ...
INFO ... GuiaAssinaturaEmailService : [SIMULAÇÃO DE E-MAIL FUSEX - GUIA ASSINADA] Para: ... | Guia nº: 202610705 ...
INFO ... GuiaAssinaturaService : Guia nº 202610705 enviada por e-mail para ...
```

### B) Olhando a Pasta `uploads/` no Computador (Arquivos PDF Físicos)
Abra a pasta `fusex/uploads/` no Windows Explorer. Você verá:
- `guia_assinatura_<numero>_<uuid>.pdf` $\rightarrow$ O PDF original que você enviou no Passo 1.
- `guia_assinada_<numero>_<uuid>.pdf` $\rightarrow$ O novo PDF gerado pelo Back-end com as assinaturas do Médico Chefe e do Chefe da Seção carimbadas no rodapé!

### C) Olhando a Tabela `guia_assinatura` no Banco de Dados
- **Se você ligou pela Opção A (Modo H2 em memória):**
  Abra em outra aba do navegador: 🔗 **`http://localhost:8080/api/h2-console`** e preencha:
  - **JDBC URL:** `jdbc:h2:mem:testdb`
  - **User Name:** `sa`
  - **Password:** *(deixe em branco)*
  Clique em **Connect**.
- **Se você ligou pela Opção B (MySQL via Docker):**
  Conecte no banco `localhost:3306`, database `fusex_db`, usuário `spring`, senha `spring` (pelo DBeaver, MySQL Workbench ou terminal).

No painel SQL, execute:
```sql
SELECT id, numero_guia, beneficiario_nome, ocs_nome, status, data_assinatura_medico, data_assinatura_secao, data_validade, data_envio_email, status_email
FROM guia_assinatura;
```
Você verá todas as guias gravadas no banco de dados com suas datas, status e validade!

---

## 6. Como Rodar os Testes Automatizados pelo Terminal (Opcional)

Se quiser rodar a bateria de testes automatizados (unitários, integração ponta a ponta e validação de banco Flyway V23) em poucos segundos, basta executar no terminal (dentro da pasta `fusex`):

```powershell
mvn test "-Dtest=GuiaAssinaturaServiceTest,GuiaAssinaturaEmailServiceTest,GuiaAssinaturaControllerTest,GuiaAssinaturaFluxoIntegracaoTest,GuiaAssinaturaFlywayTest"
```

---

## 7. Como Conectar na Tela Inicial / Login Oficial Quando o Outro Dev Terminar

Quando o outro desenvolvedor finalizar a refatoração do `login.html` e da tela inicial do Emissor de Guias, a integração será imediata (sem precisar mexer no nosso Back-end), bastando adicionar os links abaixo no menu lateral das telas dele:

- **Para o Emissor de Guias abrir a tela de Envio de Guias (US04):**
  ```html
  <a href="assinatura-guias.html?role=emitter&view=upload">Envio de guias</a>
  ```
- **Para o Emissor de Guias abrir a tela de Envio por E-mail (US06):**
  ```html
  <a href="assinatura-guias.html?role=emitter&view=email">Envio por e-mail</a>
  ```
- **Para o Médico Chefe abrir o Painel de Assinaturas (US05):**
  ```html
  <a href="assinatura-guias.html?role=med">Painel de assinaturas · Médico Chefe</a>
  ```
- **Para o Chefe da Seção abrir o Painel de Assinaturas (US05):**
  ```html
  <a href="assinatura-guias.html?role=sec">Painel de assinaturas · Chefe da Seção</a>
  ```
*(Além disso, se o login oficial gravar a credencial em `sessionStorage.setItem('fusex_auth', ...)`, a página `assinatura-guias.html` já detecta automaticamente o papel do usuário logado via `/api/auth/me`!).*

