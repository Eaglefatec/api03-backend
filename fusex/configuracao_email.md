# Guia Completo de Configuração de E-mail — FUSEX

Este documento explica detalhadamente como configurar o serviço de envio de e-mails institucionais do FUSEX para notificações de solicitações médicas aos beneficiários.

---

## 1. Visão Geral do Funcionamento

Ao criar uma solicitação médica para um paciente, o sistema gera automaticamente uma notificação por e-mail com **dupla via de acesso**:
1. **Link Direto Clicável:** Para o beneficiário que estiver acessando pelo computador ou navegador.
2. **QR Code Embutido Inline (`cid:qrcode`):** Para o beneficiário que preferir apontar a câmera do smartphone diretamente na tela do e-mail.

### Os Dois Modos de Operação do Sistema

| Modo | Configuração (`fusex.mail.enabled`) | Comportamento |
|---|---|---|
| **Simulação / Desenvolvimento (Padrão)** | `false` | Não conecta a nenhum servidor SMTP. Não falha a criação da solicitação se não houver internet. Exibe os dados e o link no console da aplicação para testes rápidos. |
| **Envio Real (Produção / Homologação)** | `true` | Conecta ao servidor SMTP configurado e dispara o e-mail HTML com o QR Code anexado para a caixa postal do paciente. |

---

## 2. Parâmetros de Configuração

As configurações dividem-se em dois blocos no arquivo `src/main/resources/application.yml`:

### A. Configurações de Negócio do FUSEX (`fusex.*`)

```yaml
fusex:
  public-base-url: ${FUSEX_PUBLIC_BASE_URL:http://localhost:8080/api}
  mail:
    enabled: ${FUSEX_MAIL_ENABLED:false}
    from: ${FUSEX_MAIL_FROM:noreply@fusex.eb.mil.br}
```

- **`fusex.mail.enabled`**: Ativa (`true`) ou desativa (`false`) o disparo real via SMTP.
- **`fusex.mail.from`**: Endereço que aparecerá como remetente oficial do e-mail (ex: `fusex.atendimento@eb.mil.br`).
- **`fusex.public-base-url`**: URL base pública pela qual o paciente acessa o sistema. É **fundamental** definir esta URL para que o link direto e o QR Code gerem o endereço correto (ex: `https://fusex.eb.mil.br/api` ou `http://192.168.1.50:8080/api`).

### B. Configurações de Conexão SMTP do Spring Boot (`spring.mail.*`)

```yaml
spring:
  mail:
    host: ${SPRING_MAIL_HOST:localhost}
    port: ${SPRING_MAIL_PORT:587}
    username: ${SPRING_MAIL_USERNAME:}
    password: ${SPRING_MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: ${SPRING_MAIL_PROPERTIES_MAIL_SMTP_AUTH:true}
          starttls:
            enable: ${SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_ENABLE:true}
            required: ${SPRING_MAIL_PROPERTIES_MAIL_SMTP_STARTTLS_REQUIRED:false}
          connectiontimeout: 5000
          timeout: 5000
          writetimeout: 5000
```

---

## 3. Como Configurar na Prática

Você pode fornecer essas credenciais de três maneiras:

### Opção 1: Via Variáveis de Ambiente (Recomendado para Segurança e Produção)

No Windows (PowerShell):
```powershell
$env:FUSEX_MAIL_ENABLED="true"
$env:FUSEX_MAIL_FROM="seu-email@dominio.com"
$env:FUSEX_PUBLIC_BASE_URL="http://seu-servidor:8080/api"
$env:SPRING_MAIL_HOST="smtp.gmail.com"
$env:SPRING_MAIL_PORT="587"
$env:SPRING_MAIL_USERNAME="seu-email@gmail.com"
$env:SPRING_MAIL_PASSWORD="sua-senha-de-app"
```

No Linux / Docker / `.env`:
```bash
FUSEX_MAIL_ENABLED=true
FUSEX_MAIL_FROM=seu-email@dominio.com
FUSEX_PUBLIC_BASE_URL=https://fusex.eb.mil.br/api
SPRING_MAIL_HOST=smtp.gmail.com
SPRING_MAIL_PORT=587
SPRING_MAIL_USERNAME=seu-email@gmail.com
SPRING_MAIL_PASSWORD=sua-senha-de-app
```

---

### Opção 2: Diretamente no `application.yml` (Apenas para Testes Locais)

Edite `src/main/resources/application.yml`:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: seu-email@gmail.com
    password: xxxx xxxx xxxx xxxx
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

fusex:
  public-base-url: http://localhost:8080/api
  mail:
    enabled: true
    from: seu-email@gmail.com
```

> **Atenção:** Nunca comite senhas ou credenciais reais em repositórios Git compartilhados.

---

## 4. Exemplos Prontos por Provedor

### Cenário 1: Usando Conta Google / Gmail

1. Acesse sua Conta Google $\rightarrow$ **Segurança** $\rightarrow$ Ative a **Verificação em duas etapas**.
2. Vá em **Senhas de app** (ou pesquise por "Senhas de app" no Google).
3. Crie uma senha com o nome `FUSEX`. O Google gerará um código de 16 letras (ex: `abcd efgh ijkl mnop`).
4. Utilize a seguinte configuração:

```yaml
spring:
  mail:
    host: smtp.gmail.com
    port: 587
    username: seu.email@gmail.com
    password: abcdefghijklmnop
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

fusex:
  mail:
    enabled: true
    from: seu.email@gmail.com
```

---

### Cenário 2: Usando Microsoft Outlook / Office 365

```yaml
spring:
  mail:
    host: smtp.office365.com
    port: 587
    username: seu-usuario@dominio.com
    password: sua-senha-ou-app-password
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true

fusex:
  mail:
    enabled: true
    from: seu-usuario@dominio.com
```

---

### Cenário 3: Servidor SMTP Próprio / Corporativo (Exército / Rede Interna)

Para servidores locais na intranet militar que não exigem SSL ou autenticação (relay interno):

```yaml
spring:
  mail:
    host: smtp.eb.mil.br
    port: 25
    username:
    password:
    properties:
      mail:
        smtp:
          auth: false
          starttls:
            enable: false

fusex:
  mail:
    enabled: true
    from: noreply.fusex@eb.mil.br
```

---

### Cenário 4: Ambiente de Testes Visual com Mailpit / MailHog (100% Gratuito e Seguro)

Para testar visualmente o layout do e-mail e o QR Code sem enviar e-mails reais:

1. Baixe o [Mailpit](https://github.com/axllent/mailpit) ou execute via Docker:
   ```bash
   docker run -d -p 8025:8025 -p 1025:1025 axllent/mailpit
   ```
2. Configure o FUSEX apontando para o Mailpit local:
   ```yaml
   spring:
     mail:
       host: localhost
       port: 1025
       properties:
         mail:
           smtp:
             auth: false
             starttls:
               enable: false

   fusex:
     mail:
       enabled: true
       from: fusex-teste@eb.mil.br
   ```
3. Abra `http://localhost:8025` no navegador para ver todos os e-mails recebidos em tempo real com imagens, botões e QR Code!

---

## 5. Diagnóstico e Resolução de Problemas (Troubleshooting)

### 1. O e-mail não chega, mas a requisição médica é salva com sucesso
- **Causa:** O envio de e-mail é projetado para ser resiliente e não interromper a emissão médica caso o servidor SMTP esteja fora.
- **O que verificar:** Observe o console da aplicação:
  - Se aparecer `[SIMULAÇÃO EMAIL] fusex.mail.enabled está desabilitado...`, significa que `fusex.mail.enabled` está como `false`.
  - Se aparecer `[ERRO EMAIL] Falha ao enviar email para...`, verifique a mensagem de erro da conexão SMTP.

### 2. Erro `535 Authentication Credentials Invalid` ou `Username and Password not accepted`
- **Causa:** Usuário ou senha incorretos. No Gmail/Google Workspace, você **não pode usar sua senha comum de login**, é obrigatório gerar uma **Senha de App de 16 caracteres**.

### 3. Erro `MailConnectException: Couldn't connect to host`
- **Causa:** Porta bloqueada pelo firewall ou provedor de internet (muitos provedores residenciais bloqueiam a porta 25; prefira sempre a porta **587** com TLS ou **465** com SSL).

### 4. O link no e-mail aponta para `localhost` e o celular do paciente não consegue abrir
- **Causa:** A propriedade `fusex.public-base-url` não foi configurada com o IP ou domínio acessível da rede.
- **Solução:** Configure `FUSEX_PUBLIC_BASE_URL=http://<IP-DA-SUA-MAQUINA>:8080/api` ou o DNS da sua rede.

