# TASK-102: REQ - ACESSO MÉDICO — Plano de Implementação

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implementar de forma robusta e limpa todas as correções e melhorias da TASK-102 (Acesso Médico) e suas subtasks (TASK-105, TASK-106, TASK-107 com envio de e-mail obrigatório contendo link direto e QR Code, e TASK-108), mantendo integridade total dos arquivos CSV oficiais e garantindo estabilidade na suíte de testes.

**Architecture:** 
- Separação de `cpf_prec` em colunas independentes `cpf` e `prec` e adição de `email` obrigatório na entidade `Paciente` e nos DTOs, com migration Flyway `V20`.
- Filtragem de procedimentos por especialidade via relacionamento JPA dinâmico (`Procedimento` -> `OcsProcedimento` -> `Ocs` -> `Especialidade`), sem qualquer alteração nos arquivos CSVs.
- Geração de QR Code via ZXing exposto via endpoint PNG e exibido visualmente no painel do médico (`medico.html`) para escaneamento imediato pelo paciente.
- Envio de e-mail institucional obrigatório ao paciente contendo **dupla via de acesso**: botão de ação com link direto clicável (`/beneficiario.html?token=...`) para quem acessa pelo computador/navegador E imagem do QR Code embutida inline (`cid:qrcode`) para quem deseja escanear pelo celular.
- Serviço de e-mail desacoplado e resiliente via `JavaMailSender` configurável (com modo mock/log em dev/testes).
- Filtragem de OCS elegíveis para o beneficiário baseada na cobertura de todos os procedimentos solicitados na requisição, com endpoint dedicado e validação transacional na triagem.

**Tech Stack:** Java 21, Spring Boot 3.4.13, Spring Data JPA, Flyway, ZXing 3.5.3 (QR Code), Spring Boot Starter Mail (JavaMailSender), H2 / MySQL, JUnit 5, Mockito, HTML5/CSS/Vanilla JS.

**Spec:** Jira TASK-102 e Subtasks TASK-105, TASK-106, TASK-107 (+ adendo de e-mail obrigatório com link direto + QR Code), TASK-108.

---

## Restrições Críticas Não Negociáveis

1. **CSVs Oficiais Intocáveis:** Os arquivos `ocs_precos.csv`, `tuss_procedimentos.csv` e `ocs_cadastro.csv` **NÃO PODEM SER ALTERADOS**. Toda a lógica de relacionamento deve ser derivada do banco de dados e dos loaders existentes.
2. **Migrations Históricas Intocáveis:** Não editar as migrations existentes `V1` até `V19`. Qualquer alteração de DDL/DML deve ser feita exclusivamente na nova migration `V20`.
3. **E-mail Obrigatório com Duplo Acesso:** O campo `email` é obrigatório para o paciente. O e-mail enviado deve conter obrigatoriamente:
   - Link direto clicável (botão de destaque e URL explícita) para acesso imediato no computador;
   - Imagem do QR Code embutida inline (`cid:qrcode`) para leitura via smartphone.
   - Os testes do Maven não devem depender de conexão real com servidor SMTP externo (usar profile/mocking seguro).
4. **QR Code Visual na Tela do Médico:** O QR Code deve ser renderizado diretamente na tela do médico (`medico.html`), permitindo que o beneficiário escaneie na hora com a câmera do smartphone ou que o médico imprima, além do link copiável.
5. **Cobertura Integral da OCS:** Uma OCS só pode ser listada para o beneficiário se possuir preços cadastrados para **todos** os procedimentos solicitados na requisição médica.

---

## Estrutura de Arquivos

### Arquivos a Modificar
- `pom.xml`: Adicionar dependências `zxing` e `spring-boot-starter-mail`.
- `src/main/resources/application.yml`: Configurações de e-mail e URL pública do QR Code.
- `src/main/resources/db/migration/V20__separar_cpf_prec_e_adicionar_email.sql`: Nova migration.
- `src/main/java/com/eagle/fusex/model/entity/Paciente.java`: Campos `cpf`, `prec`, `email` (remoção de `cpfPrec`).
- `src/main/java/com/eagle/fusex/model/dto/request/CriarSolicitacaoRequest.java`: Campos `cpf`, `prec`, `email`.
- `src/main/java/com/eagle/fusex/model/dto/request/CadastrarPacienteRequest.java`: Campos `cpf`, `prec`, `email`.
- `src/main/java/com/eagle/fusex/model/dto/request/PreencherTriagemRequest.java`: Campos `cpf`, `prec`.
- `src/main/java/com/eagle/fusex/model/dto/response/PacienteResponse.java`: Campos `cpf`, `prec`, `email`.
- `src/main/java/com/eagle/fusex/model/dto/response/SolicitacaoResponse.java`: Campo `emailEnviado`, `qrCodeUrl`.
- `src/main/java/com/eagle/fusex/model/dto/response/SolicitacaoPublicaResponse.java`: Campos `cpf`, `prec`, `email`.
- `src/main/java/com/eagle/fusex/model/dto/response/PreGuiaConsolidadaResponse.java`: Subclasse `Beneficiario` com `cpf`, `prec`, `email`.
- `src/main/java/com/eagle/fusex/repository/PacienteRepository.java`: Buscar por CPF ou PREC.
- `src/main/java/com/eagle/fusex/repository/ProcedimentoRepository.java`: Query com filtro por especialidades.
- `src/main/java/com/eagle/fusex/repository/OcsRepository.java`: Query de OCS por procedimentos requeridos.
- `src/main/java/com/eagle/fusex/service/ProcedimentoService.java`: Suporte a filtro por especialidades.
- `src/main/java/com/eagle/fusex/service/OcsService.java`: Método para listar OCS por conjunto de procedimentos.
- `src/main/java/com/eagle/fusex/service/SolicitacaoMedicaService.java`: Persistência de CPF/PREC/E-mail e disparo de e-mail.
- `src/main/java/com/eagle/fusex/service/TriagemPreGuiaService.java`: Validação da OCS e suporte a CPF/PREC.
- `src/main/java/com/eagle/fusex/controller/ProcedimentoController.java`: Parâmetro `especialidades`.
- `src/main/java/com/eagle/fusex/controller/SolicitacaoPublicoController.java`: Endpoints `/qrcode` e `/ocs-disponiveis`.
- `src/main/resources/static/medico.html`: Campos separados de CPF/PREC/E-mail, autocomplete com especialidade, card de QR Code visual e link.
- `src/main/resources/static/beneficiario.html`: Campos separados de CPF/PREC, carregamento de OCS por procedimentos da solicitação.
- `src/main/resources/static/guia.html`: Exibição de CPF, PREC e E-mail.
- Testes: `SolicitacaoMedicaServiceTest`, `TriagemPreGuiaServiceTest`, `SolicitacaoMedicoControllerTest`, etc.

### Novos Arquivos a Criar
- `src/main/java/com/eagle/fusex/service/QrCodeService.java`: Utilitário para gerar imagem PNG e Base64 do QR Code via ZXing.
- `src/main/java/com/eagle/fusex/service/SolicitacaoEmailService.java`: Serviço para montagem e envio de e-mail institucional com botão de link direto E QR Code inline.
- `src/test/java/com/eagle/fusex/service/QrCodeServiceTest.java`: Teste unitário de geração de QR Code.
- `src/test/java/com/eagle/fusex/service/SolicitacaoEmailServiceTest.java`: Teste unitário de envio de e-mail com mock (validando presença do link e do QR Code inline).

---

## Plano de Tarefas

### Fase 1: Dependências e Configuração de Infraestrutura
- [ ] **Tarefa 1.1: Adicionar dependências no `pom.xml`**
  - Adicionar `com.google.zxing:core:3.5.3` e `com.google.zxing:javase:3.5.3`.
  - Adicionar `org.springframework.boot:spring-boot-starter-mail`.
  - Atualizar dependências e validar compilação limpa.
- [ ] **Tarefa 1.2: Configurar propriedades em `application.yml`**
  - Adicionar bloco `fusex.public-base-url` (padrão: `http://localhost:8080/api`).
  - Adicionar bloco `fusex.mail` com `enabled: ${FUSEX_MAIL_ENABLED:false}`, `from: noreply@fusex.eb.mil.br`.
  - Em testes, garantir que `fusex.mail.enabled` permaneça desabilitado ou mockado por padrão.

### Fase 2: TASK-105 — Separação de CPF e PREC + E-mail Obrigatório
- [ ] **Tarefa 2.1: Criar migration `V20__separar_cpf_prec_e_adicionar_email.sql`**
  - Adicionar colunas `cpf VARCHAR(14)`, `prec VARCHAR(30)` e `email VARCHAR(255)` na tabela `paciente`.
  - Migrar dados legados: `UPDATE paciente SET cpf = cpf_prec, prec = cpf_prec, email = 'paciente@fusex.eb.mil.br' WHERE cpf IS NULL;`.
  - Aplicar `NOT NULL` nas novas colunas e criar índices: `idx_paciente_cpf`, `idx_paciente_prec`.
  - Remover a coluna legada: `ALTER TABLE paciente DROP COLUMN cpf_prec;`.
- [ ] **Tarefa 2.2: Refatorar entidade `Paciente` e Repositório**
  - Substituir campo `cpfPrec` pelos campos `cpf`, `prec` e `email`.
  - Implementar normalização de CPF (remover caracteres não numéricos para pesquisa/armazenamento) e trimming de PREC/Email.
  - No `PacienteRepository`: substituir `findByCpfPrec` por `findByCpf`, `findByPrec` e `findByCpfOrPrec`.
- [ ] **Tarefa 2.3: Atualizar DTOs de Paciente e Solicitação**
  - Atualizar `CriarSolicitacaoRequest`: campos `cpf`, `prec`, `email` com validação de presença e formato.
  - Atualizar `CadastrarPacienteRequest`: campos `cpf`, `prec`, `email`.
  - Atualizar `PacienteResponse`: expor `cpf`, `prec`, `email`.
  - Atualizar `PreencherTriagemRequest`: campos `cpf`, `prec`.
  - Atualizar `SolicitacaoPublicaResponse`: expor `cpf`, `prec`, `email`.
  - Atualizar `PreGuiaConsolidadaResponse.Beneficiario`: expor `cpf`, `prec`, `email` e método auxiliar de compatibilidade `getCpfPrec()`.
- [ ] **Tarefa 2.4: Atualizar Services e Controllers de Solicitação e Triagem**
  - Em `SolicitacaoMedicaService`: buscar paciente por CPF ou PREC, salvar novos dados e associar e-mail.
  - Em `TriagemPreGuiaService`: atualizar CPF e PREC no paciente durante a triagem.
  - Em `PreGuiaConsolidadaService`: mapear CPF, PREC e E-mail.
  - Em `PacienteService`: cadastrar paciente com CPF, PREC e E-mail validados.
- [ ] **Tarefa 2.5: Atualizar testes existentes para refletir CPF, PREC e E-mail**
  - Atualizar `SolicitacaoMedicaServiceTest`, `TriagemPreGuiaServiceTest`, `SolicitacaoMedicoControllerTest`, `PacienteServiceTest`.

### Fase 3: TASK-106 — Filtragem de Procedimentos por Especialidade
- [ ] **Tarefa 3.1: Criar consulta dinâmica de procedimentos por especialidade no `ProcedimentoRepository`**
  - Adicionar query JPQL utilizando o relacionamento existente com `OcsProcedimento` e `Ocs.especialidades`:
    ```java
    @Query("""
        SELECT DISTINCT p FROM Procedimento p
        JOIN p.ocsProcedimentos op
        JOIN op.ocs o
        JOIN o.especialidades esp
        WHERE LOWER(p.procDescricao) LIKE LOWER(CONCAT('%', :busca, '%'))
          AND esp IN (:especialidades)
        ORDER BY p.procDescricao
    """)
    List<Procedimento> findTop20ByDescricaoAndEspecialidades(
        @Param("busca") String busca,
        @Param("especialidades") Collection<Especialidade> especialidades
    );
    ```
- [ ] **Tarefa 3.2: Atualizar `ProcedimentoService` e `ProcedimentoController`**
  - No `ProcedimentoService.listarProcedimentos`: aceitar `Set<Especialidade> especialidades`.
  - Se especialidades fornecidas: consultar filtrando por especialidade; se vazio/nulo: manter busca geral.
  - No `ProcedimentoController`: adicionar `@RequestParam(required = false) Set<Especialidade> especialidades`.
- [ ] **Tarefa 3.3: Escrever testes unitários e de integração para a busca filtrada**
  - Testar que procedimentos de cardiologia NÃO são retornados quando filtrado por ortopedia.
  - Testar que a busca sem especialidade continua retornando resultados normalmente.

### Fase 4: TASK-107 — Geração de QR Code + Envio de E-mail com Link Direto e QR Code
- [ ] **Tarefa 4.1: Implementar `QrCodeService`**
  - Implementar método para gerar `byte[]` de imagem PNG via ZXing.
  - Implementar método para gerar Base64 Data URL (`data:image/png;base64,...`).
  - Implementar resolução da URL pública do beneficiário: `${publicBaseUrl}/beneficiario.html?token=${token}`.
  - Criar teste unitário `QrCodeServiceTest` validando a integridade da imagem gerada.
- [ ] **Tarefa 4.2: Criar endpoint `GET /api/solicitacoes/publico/{token}/qrcode`**
  - Em `SolicitacaoPublicoController`, expor endpoint retornando `MediaType.IMAGE_PNG_VALUE`.
- [ ] **Tarefa 4.3: Implementar `SolicitacaoEmailService` com Link Direto + QR Code**
  - Injetar `ObjectProvider<JavaMailSender>` e checar se `fusex.mail.enabled` está ativo.
  - Montar layout HTML institucional do e-mail com **dupla via de acesso**:
    - **Acesso direto via PC/Desktop:** Botão de destaque estilizado (*"Acessar Minha Solicitação Médica"*) e link textual completo clicável direcionando para `${publicBaseUrl}/beneficiario.html?token=${token}`.
    - **Acesso via Celular/Tablet:** Imagem do QR Code embutida inline (`cid:qrcode`), permitindo escaneamento direto com a câmera.
    - Instruções claras: *"Se você está no computador, clique no botão acima para abrir sua solicitação. Se preferir acessar pelo celular, aponte a câmera para o QR Code abaixo."*
  - Se inativo (dev/test): logar estruturadamente o envio simulado com a URL e dados sem falhar a criação.
  - Criar teste unitário `SolicitacaoEmailServiceTest` com mock do `JavaMailSender`, verificando a presença do link clicável e do anexo inline `cid:qrcode`.
- [ ] **Tarefa 4.4: Integrar QR Code e E-mail no `SolicitacaoMedicaService`**
  - Disparar envio de e-mail ao criar solicitação médica.
  - Adicionar dados de retorno no `SolicitacaoResponse` (`emailEnviado`, `qrCodeUrl`).

### Fase 5: TASK-108 — Filtragem de OCS Disponíveis por Procedimentos
- [ ] **Tarefa 5.1: Implementar consulta de OCS elegíveis no `OcsRepository`**
  - Criar query que retorna OCSs que possuem preços para **todos** os procedimentos solicitados:
    ```java
    @Query("""
        SELECT o FROM Ocs o
        WHERE o.ocsId IN (
            SELECT op.ocs.ocsId FROM OcsProcedimento op
            WHERE op.procedimento.procCodigoDgp IN (:codigos)
            GROUP BY op.ocs.ocsId
            HAVING COUNT(DISTINCT op.procedimento.procCodigoDgp) = :totalProcedimentos
        )
        ORDER BY o.ocsNome ASC
    """)
    List<Ocs> findOcsQueAtendemTodosProcedimentos(
        @Param("codigos") Collection<String> codigos,
        @Param("totalProcedimentos") long totalProcedimentos
    );
    ```
- [ ] **Tarefa 5.2: Adicionar método no `OcsService` e endpoint público**
  - Em `OcsService`: `List<OcsResponse> listarPorProcedimentosDaSolicitacao(String token)`.
  - Em `SolicitacaoPublicoController`: expor `GET /api/solicitacoes/publico/{token}/ocs-disponiveis`.
- [ ] **Tarefa 5.3: Validar cobertura da OCS no `TriagemPreGuiaService.preencherTriagem`**
  - Antes de persistir a triagem, revalidar se a `ocsId` informada no request realmente atende todos os procedimentos solicitados.
  - Lançar `OcsInvalidoException` com mensagem clara caso a OCS não atenda.
- [ ] **Tarefa 5.4: Testes unitários para elegibilidade de OCS**
  - Testar cenário com OCS que atende a todos os procedimentos (sucesso).
  - Testar cenário com OCS que só atende parte dos procedimentos (rejeição).

### Fase 6: Ajustes nas Telas (Frontend)
- [ ] **Tarefa 6.1: Atualizar `medico.html`**
  - Separar os inputs do paciente: Nome, OM, CPF (com máscara), PREC e E-mail.
  - No autocomplete de procedimentos TUSS: passar `&especialidades=` com as especialidades marcadas no momento da digitação.
  - No card de sucesso pós-criação:
    - Renderizar visualmente a imagem do QR Code (`<img id="qrCodeImg" ...>`).
    - Exibir o link direto com botão de copiar.
    - Exibir botão para imprimir / salvar o QR Code.
    - Exibir status do envio do e-mail ao beneficiário.
- [ ] **Tarefa 6.2: Atualizar `beneficiario.html`**
  - Separar campos de CPF e PREC no formulário de triagem.
  - Alterar busca de clínicas credenciadas: chamar `GET /api/solicitacoes/publico/{token}/ocs-disponiveis` em vez de buscar apenas por especialidade.
  - Exibir alerta amigável caso nenhuma OCS atenda a todos os exames solicitados.
- [ ] **Tarefa 6.3: Atualizar `guia.html`**
  - Separar a linha de "CPF / PREC" em linhas individuais para CPF, PREC e E-mail.

### Fase 7: Verificação Integrada e Homologação
- [ ] **Tarefa 7.1: Executar suíte completa de testes (`mvn test`)**
  - Garantir 0 failures e 0 errors.
- [ ] **Tarefa 7.2: Teste de ponta a ponta do fluxo**
  - Médico cria solicitação (com CPF, PREC, E-mail, especialidade e procedimentos filtrados).
  - Médico visualiza QR Code na tela e link.
  - E-mail é gerado com link clicável de redirecionamento e QR Code inline.
  - Beneficiário acessa via link ou QR Code.
  - Beneficiário vê apenas OCSs que realizam os procedimentos solicitados.
  - Triagem concluída e pré-guia gerada com dados completos.
- [ ] **Tarefa 7.3: Conferência final dos arquivos CSVs**
  - Garantir que `git status` não reporte nenhuma alteração nos CSVs.
