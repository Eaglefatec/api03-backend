# Resiliência de E-mail: Diagnóstico, Agendamento e Reenvio Manual

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar arquitetura resiliente para disparo de e-mails de solicitações médicas no FUSEX, garantindo que falhas de conexão/rede ou quedas de SMTP resultem em retentativa automática em background (scheduler), fornecendo diagnósticos detalhados de status e permitindo ao médico corrigir o e-mail ou solicitar reenvio imediato diretamente pela interface.

---

### Task 1: Banco de Dados e Modelagem de Status de E-mail
- [x] Criar migration `V22__add_status_email_solicitacao.sql` adicionando colunas:
  - `status_email VARCHAR(30) NOT NULL DEFAULT 'SIMULADO'`
  - `email_destinatario VARCHAR(255) NULL`
  - `email_tentativas INT NOT NULL DEFAULT 0`
  - `email_ultimo_erro VARCHAR(500) NULL`
  - `email_proxima_tentativa DATETIME NULL`
  - Índice em `status_email`
- [x] Criar enum `StatusEmail` com valores `ENVIADO`, `PENDENTE_REENVIO`, `FALHA_ENVIO`, `SIMULADO`.
- [x] Atualizar entidade `SolicitacaoMedica` com os novos atributos, getters e setters.
- [x] Atualizar `SolicitacaoMedicaRepository` com consulta para buscar itens pendentes de reenvio:
  - `findByStatusEmailAndEmailProximaTentativaLessThanEqualAndEmailTentativasLessThan(...)`.

### Task 2: Classificação Inteligente de Erros no Envio de E-mail
- [x] Criar classe de resultado `ResultadoEnvioEmail(boolean sucesso, StatusEmail status, String mensagemErro, boolean retentavel)`.
- [x] Refatorar `SolicitacaoEmailService` para retornar `ResultadoEnvioEmail`:
  - Se `!mailEnabled`: retorna `SIMULADO`.
  - Se e-mail em branco/inválido: retorna `FALHA_ENVIO` (não retentável).
  - Se exceção de conexão/timeout/rede (`MailConnectException`, `SocketTimeoutException`, etc.): retorna `PENDENTE_REENVIO` (retentável).
  - Se sucesso: retorna `ENVIADO`.
- [x] Criar testes unitários em `SolicitacaoEmailServiceTest` cobrindo cada cenário.

### Task 3: Serviço e Endpoint de Reenvio Manual e Atualização de E-mail
- [x] Atualizar DTOs:
  - Criar `ReenviarEmailRequest(String novoEmail)`.
  - Atualizar `SolicitacaoResponse` com `statusEmail`, `emailDestinatario`, `emailMensagem`.
- [x] Implementar método `reenviarEmail(Long solicitacaoId, String novoEmail)` em `SolicitacaoMedicaService`:
  - Atualiza e-mail do paciente se fornecido.
  - Zera contador de tentativas e dispara novo envio imediato.
  - Atualiza status e campos na `SolicitacaoMedica`.
- [x] Adicionar endpoint `POST /solicitacoes/{id}/reenviar-email` em `SolicitacaoMedicoController`.
- [x] Criar testes unitários para o fluxo de reenvio manual e alteração de e-mail.

### Task 4: Agendador em Background para Auto-Recuperação (Retry Scheduler)
- [x] Habilitar agendamento com `@EnableScheduling` na classe `FusexApplication`.
- [x] Criar classe `EmailRetryScheduler`:
  - `@Scheduled(fixedDelay = 60000)` (executa a cada 1 minuto).
  - Busca solicitações com status `PENDENTE_REENVIO` e `emailProximaTentativa <= now()`.
  - Reexecuta o envio; se suceder, atualiza status para `ENVIADO`.
  - Se falhar novamente, calcula backoff exponencial (`proximaTentativa = now + (tentativas * 2) min`). Se exceder 5 tentativas, marca como `FALHA_ENVIO`.
- [x] Criar teste unitário em `EmailRetrySchedulerTest`.

### Task 5: Interface do Médico — Diagnóstico e Painel de Reenvio Interativo
- [x] Atualizar [`medico.html`](file:///C:/Users/dasmi/IdeaProjects/api03/fusex/src/main/resources/static/medico.html):
  - Exibir badge com o status exato do e-mail retornado pela API.
  - Se o e-mail não estiver `ENVIADO`, renderizar área interativa com:
    - Input preenchido com o e-mail atual do paciente permitindo edição rápida.
    - Botão "🔄 Reenviar E-mail" / "Salvar e Reenviar".
    - Indicador de carregamento e mensagem de feedback live ao reprocessar.
- [x] Testar integração de ponta a ponta e verificar suite de testes.
