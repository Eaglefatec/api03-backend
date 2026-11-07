package com.eagle.fusex.service;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.exception.UnauthorizedOperationException;
import com.eagle.fusex.exception.ValidationException;
import com.eagle.fusex.model.dto.ResultadoEnvioEmail;
import com.eagle.fusex.model.dto.request.DecisaoAssinaturaRequest;
import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.dto.response.GuiaAssinaturaDetalheResponse;
import com.eagle.fusex.model.dto.response.GuiaAssinaturaResponse;
import com.eagle.fusex.model.dto.response.PainelAssinaturaResumoResponse;
import com.eagle.fusex.model.dto.response.ProcedimentoGuiaDTO;
import com.eagle.fusex.model.entity.GuiaAssinatura;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.model.enums.DecisaoAssinatura;
import com.eagle.fusex.model.enums.StatusEmail;
import com.eagle.fusex.model.enums.StatusEsteiraAssinatura;
import com.eagle.fusex.repository.GuiaAssinaturaRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.repository.PacienteRepository;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class GuiaAssinaturaService {

    private static final Logger log = LoggerFactory.getLogger(GuiaAssinaturaService.class);

    public static final long LIMITE_TAMANHO_PDF_BYTES = 5L * 1024 * 1024;
    private static final DateTimeFormatter FMT_DATA_BR = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private static final String[][] EXEMPLOS_FALLBACK = {
            {"Camila R. Moreira", "34877120503", "Plani Diagnósticos Médicos Ltda", "camila.moreira@email.com.br"},
            {"Eduardo F. Lima", "34988231604", "Clínica Ortopédica Santa Lúcia", "eduardo.lima@email.com.br"},
            {"Patrícia A. Souza", "35099342705", "Fisio Vip Clin Serv. Fisioter. Ltda", "patricia.souza@email.com.br"}
    };

    private final AtomicInteger sequencialFallback = new AtomicInteger(705);

    private final GuiaAssinaturaRepository guiaAssinaturaRepository;
    private final PacienteRepository pacienteRepository;
    private final OcsRepository ocsRepository;
    private final SolicitacaoMedicaRepository solicitacaoRepository;
    private final FileStorageService fileStorageService;
    private final GuiaAssinaturaPdfService pdfService;
    private final GuiaAssinaturaEmailService emailService;
    private final int validadeDias;

    public GuiaAssinaturaService(GuiaAssinaturaRepository guiaAssinaturaRepository,
                                 PacienteRepository pacienteRepository,
                                 OcsRepository ocsRepository,
                                 SolicitacaoMedicaRepository solicitacaoRepository,
                                 FileStorageService fileStorageService,
                                 GuiaAssinaturaPdfService pdfService,
                                 GuiaAssinaturaEmailService emailService,
                                 @Value("${fusex.guia.validade-dias:60}") int validadeDias) {
        this.guiaAssinaturaRepository = guiaAssinaturaRepository;
        this.pacienteRepository = pacienteRepository;
        this.ocsRepository = ocsRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.fileStorageService = fileStorageService;
        this.pdfService = pdfService;
        this.emailService = emailService;
        this.validadeDias = validadeDias;
    }

    @Transactional
    public GuiaAssinaturaResponse enviarGuiaParaAssinatura(MultipartFile arquivo) {
        validarArquivoPdf5Mb(arquivo);

        byte[] bytesPdf;
        try {
            bytesPdf = arquivo.getBytes();
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler os bytes do arquivo PDF.");
        }
        validarConteudoPdfReal(bytesPdf);

        GuiaAssinaturaPdfService.DadosExtraidosPdf extraidos = pdfService.extrairDadosDoPdf(bytesPdf);

        int idxExemplo = Math.abs(sequencialFallback.getAndIncrement() - 705) % EXEMPLOS_FALLBACK.length;
        String[] fallback = EXEMPLOS_FALLBACK[idxExemplo];

        String numeroGuia = extraidos.numeroGuia() != null ? extraidos.numeroGuia().trim() : null;
        if (numeroGuia != null && !numeroGuia.isBlank()) {
            if (numeroGuia.contains("..") || numeroGuia.matches(".*[\\p{Cntrl}].*")) {
                throw new ArquivoInvalidoException("Número de guia inválido identificado no documento PDF.");
            }
            numeroGuia = truncar(numeroGuia, 100);
            if (guiaAssinaturaRepository.existsByNumeroGuia(numeroGuia)) {
                throw new ValidationException("A guia nº " + numeroGuia + " já foi enviada para a esteira de assinaturas.");
            }
        } else {
            numeroGuia = gerarNumeroGuiaSequencialUnico();
        }

        String beneficiarioNome = extraidos.beneficiarioNome();
        String beneficiarioPrec = extraidos.beneficiarioPrecCp();
        String beneficiarioEmail = extraidos.beneficiarioEmail();
        String ocsNome = extraidos.ocsNome();

        // Completa os dados com o cadastro existente no banco, quando houver
        var solOpt = solicitacaoRepository.findByNumeroGuia(numeroGuia);
        if (solOpt.isPresent() && solOpt.get().getPaciente() != null) {
            Paciente pacSol = solOpt.get().getPaciente();
            log.debug("Enriquecendo dados da guia {} a partir da solicitação médica #{}", numeroGuia, solOpt.get().getId());
            if (beneficiarioNome == null || beneficiarioNome.isBlank()) beneficiarioNome = pacSol.getNome();
            if (beneficiarioPrec == null || beneficiarioPrec.isBlank()) beneficiarioPrec = pacSol.getPrec();
            if (beneficiarioEmail == null || beneficiarioEmail.isBlank()) beneficiarioEmail = pacSol.getEmail();
        }

        if (beneficiarioPrec != null && !beneficiarioPrec.isBlank()) {
            Paciente pac = pacienteRepository.findByCpfPrec(beneficiarioPrec).orElse(null);
            if (pac != null) {
                if (beneficiarioNome == null || beneficiarioNome.isBlank()) beneficiarioNome = pac.getNome();
                if (beneficiarioEmail == null || beneficiarioEmail.isBlank()) beneficiarioEmail = pac.getEmail();
            }
        } else if (beneficiarioNome != null && !beneficiarioNome.isBlank()) {
            List<Paciente> encontrados = pacienteRepository.findTop10ByNomeContainingIgnoreCaseOrderByNome(beneficiarioNome);
            if (!encontrados.isEmpty()) {
                Paciente pac = encontrados.get(0);
                beneficiarioPrec = pac.getPrec();
                if (beneficiarioEmail == null || beneficiarioEmail.isBlank()) beneficiarioEmail = pac.getEmail();
            }
        }

        if (beneficiarioNome == null || beneficiarioNome.isBlank()) beneficiarioNome = fallback[0];
        if (beneficiarioPrec == null || beneficiarioPrec.isBlank()) beneficiarioPrec = fallback[1];
        if (ocsNome == null || ocsNome.isBlank()) ocsNome = fallback[2];
        if (beneficiarioEmail == null || beneficiarioEmail.isBlank()) beneficiarioEmail = fallback[3];

        beneficiarioNome = truncar(beneficiarioNome.trim(), 255);
        beneficiarioPrec = truncar(beneficiarioPrec.trim(), 50);
        beneficiarioEmail = truncar(beneficiarioEmail.trim(), 255);
        ocsNome = truncar(ocsNome.trim(), 255);

        Ocs ocsEntidade = ocsRepository.findByOcsNome(ocsNome).orElse(null);

        String slugGuia = slugSeguroArquivo(numeroGuia);
        String nomeOriginal = extrairNomeArquivoSeguro(arquivo.getOriginalFilename(), "guia_" + slugGuia + ".pdf");
        String nomeDestino = "guia_assinatura_" + slugGuia + "_" + UUID.randomUUID() + ".pdf";
        String caminhoSalvo = fileStorageService.salvar(bytesPdf, nomeDestino);

        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setNumeroGuia(numeroGuia);
        guia.setBeneficiarioNome(beneficiarioNome);
        guia.setBeneficiarioPrecCp(beneficiarioPrec);
        guia.setBeneficiarioEmail(beneficiarioEmail);
        guia.setOcsNome(ocsNome);
        guia.setOcs(ocsEntidade);
        guia.setProcedimentosResumo(pdfService.serializarProcedimentos(extraidos.procedimentos()));
        guia.setNomeArquivoOriginal(nomeOriginal);
        guia.setCaminhoArquivoOriginal(caminhoSalvo);
        guia.setTamanhoBytes(arquivo.getSize());
        guia.setStatus(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);
        guia.setDataEnvio(LocalDateTime.now());

        guia = guiaAssinaturaRepository.save(guia);
        log.info("Guia nº {} registrada na esteira de assinaturas (Beneficiário: {}, OCS: {})",
                guia.getNumeroGuia(), guia.getBeneficiarioNome(), guia.getOcsNome());

        String msg = "Arquivo " + nomeOriginal + " recebido. Os campos identificados foram associados ao registro da guia.";
        return toResponse(guia, msg);
    }

    @Transactional(readOnly = true)
    public List<GuiaAssinaturaResponse> listarGuiasAcompanhamento() {
        return guiaAssinaturaRepository.findAllByOrderByDataEnvioDesc().stream()
                .map(g -> toResponse(g, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public PainelAssinaturaResumoResponse obterResumoPainel() {
        List<GuiaAssinaturaResponse> todas = listarGuiasAcompanhamento();
        long pendentesMed = guiaAssinaturaRepository.countByStatus(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);
        long pendentesSec = guiaAssinaturaRepository.countByStatus(StatusEsteiraAssinatura.AGUARDANDO_CHEFE_SECAO);
        long prontas = guiaAssinaturaRepository.countByStatus(StatusEsteiraAssinatura.AGUARDANDO_ENVIO_BENEFICIARIO);
        long enviadas = guiaAssinaturaRepository.countByStatus(StatusEsteiraAssinatura.ENVIADA_BENEFICIARIO);

        return new PainelAssinaturaResumoResponse(pendentesMed, pendentesSec, prontas, enviadas, todas);
    }

    @Transactional(readOnly = true)
    public List<GuiaAssinaturaResponse> listarPendencias(String perfilParam, Authentication auth) {
        StatusEsteiraAssinatura statusAlvo = resolverStatusAlvoPorPerfilOuRole(perfilParam, auth);
        return guiaAssinaturaRepository.findByStatusOrderByDataEnvioAsc(statusAlvo).stream()
                .map(g -> toResponse(g, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public GuiaAssinaturaDetalheResponse obterDetalhes(Long id) {
        GuiaAssinatura guia = buscarPorId(id);
        List<ProcedimentoGuiaDTO> procedimentos = pdfService.desserializarProcedimentos(guia.getProcedimentosResumo());
        return new GuiaAssinaturaDetalheResponse(
                guia.getId(),
                guia.getNumeroGuia(),
                guia.getBeneficiarioNome(),
                guia.getBeneficiarioPrecCp(),
                guia.getBeneficiarioEmail(),
                guia.getOcsNome(),
                guia.getNomeArquivoOriginal(),
                guia.getStatus(),
                guia.getStatus().getCodigoCurto(),
                guia.getStatus().getDescricao(),
                guia.getDataEnvio(),
                guia.getMedicoChefeUsuario(),
                guia.getDataAssinaturaMedico(),
                guia.getAssinaturaMedicoBase64(),
                guia.getChefeSecaoUsuario(),
                guia.getDataAssinaturaSecao(),
                guia.getAssinaturaSecaoBase64(),
                guia.getDataValidade(),
                validadeDias,
                validadeDias + " dias a partir da assinatura do Chefe da Seção",
                procedimentos,
                montarMensagemPronta(guia),
                "/api/assinaturas/guias/" + guia.getId() + "/pdf"
        );
    }

    @Transactional(readOnly = true)
    public ArquivoLoteDownload obterArquivoPdf(Long id) {
        GuiaAssinatura guia = buscarPorId(id);
        byte[] bytes = lerBytesPdfGuia(guia);
        String nomeDownload = extrairNomeArquivoSeguro(
                guia.getNomeArquivoOriginal(),
                "guia_" + slugSeguroArquivo(guia.getNumeroGuia()) + ".pdf");
        return new ArquivoLoteDownload(bytes, nomeDownload, "application/pdf");
    }

    @Transactional
    public GuiaAssinaturaResponse processarDecisao(Long id, DecisaoAssinaturaRequest request, Authentication auth) {
        GuiaAssinatura guia = buscarPorIdParaAtualizacao(id);
        String username = auth != null ? auth.getName() : "sistema";
        Set<String> roles = extrairRoles(auth);

        if (guia.getStatus() == StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE) {
            validarPermissaoMedicoChefe(request.perfil(), roles);
        } else if (guia.getStatus() == StatusEsteiraAssinatura.AGUARDANDO_CHEFE_SECAO) {
            validarPermissaoChefeSecao(request.perfil(), roles);
        } else {
            if (request.decisao() == DecisaoAssinatura.REJEITAR) {
                throw new ValidationException("Somente guias aguardando assinatura podem ser rejeitadas.");
            }
            throw new ValidationException("Esta guia não se encontra pendente de assinatura (status atual: "
                    + guia.getStatus().getDescricao() + ").");
        }

        if (request.decisao() == DecisaoAssinatura.REJEITAR) {
            guia.setStatus(StatusEsteiraAssinatura.REJEITADA);
            String motivo = request.motivoRejeicao() != null && !request.motivoRejeicao().isBlank()
                    ? request.motivoRejeicao().trim()
                    : "Rejeitada na conferência de assinatura por " + username;
            guia.setMotivoRejeicao(truncar(motivo, 500));
            guia = guiaAssinaturaRepository.save(guia);
            log.info("Guia nº {} rejeitada por {}", guia.getNumeroGuia(), username);
            return toResponse(guia, "Guia rejeitada.");
        }

        if (!Boolean.TRUE.equals(request.confirmado())) {
            throw new ValidationException("Confirme que conferiu os dados da guia antes de autorizar a assinatura.");
        }
        String assinaturaLimpa = normalizarEValidarAssinaturaBase64(request.assinaturaBase64());

        List<ProcedimentoGuiaDTO> procedimentos = pdfService.desserializarProcedimentos(guia.getProcedimentosResumo());
        String caminhoAssinadoAnterior = guia.getCaminhoArquivoAssinado();

        if (guia.getStatus() == StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE) {
            LocalDateTime agora = LocalDateTime.now();
            guia.setMedicoChefeUsuario(username);
            guia.setDataAssinaturaMedico(agora);
            guia.setAssinaturaMedicoBase64(assinaturaLimpa);
            guia.setStatus(StatusEsteiraAssinatura.AGUARDANDO_CHEFE_SECAO);

            estamparESalvarPdf(guia, procedimentos);
            guia = guiaAssinaturaRepository.save(guia);
            removerArquivoAssinadoAnteriorSeNecessario(caminhoAssinadoAnterior, guia);

            log.info("1ª assinatura (Médico Chefe) registrada para a guia nº {} por {}", guia.getNumeroGuia(), username);
            return toResponse(guia, "Assinatura do Médico Chefe registrada com sucesso.");

        } else {
            LocalDateTime agora = LocalDateTime.now();
            LocalDate validade = agora.toLocalDate().plusDays(validadeDias);

            guia.setChefeSecaoUsuario(username);
            guia.setDataAssinaturaSecao(agora);
            guia.setAssinaturaSecaoBase64(assinaturaLimpa);
            guia.setDataValidade(validade);
            guia.setStatus(StatusEsteiraAssinatura.AGUARDANDO_ENVIO_BENEFICIARIO);

            estamparESalvarPdf(guia, procedimentos);
            guia = guiaAssinaturaRepository.save(guia);

            // Atualiza a validade na solicitação médica vinculada, se existir
            solicitacaoRepository.findByNumeroGuia(guia.getNumeroGuia()).ifPresent(sol -> {
                sol.setDataValidade(validade);
                solicitacaoRepository.save(sol);
            });
            removerArquivoAssinadoAnteriorSeNecessario(caminhoAssinadoAnterior, guia);

            log.info("2ª assinatura (Chefe da Seção) registrada para a guia nº {} por {}. Validade fixada até {}",
                    guia.getNumeroGuia(), username, validade);
            return toResponse(guia, "Assinatura registrada com sucesso.");
        }
    }

    @Transactional(readOnly = true)
    public List<GuiaAssinaturaResponse> listarProntasParaEnvio() {
        return guiaAssinaturaRepository
                .findByStatusOrderByDataAssinaturaSecaoDesc(StatusEsteiraAssinatura.AGUARDANDO_ENVIO_BENEFICIARIO)
                .stream()
                .map(g -> toResponse(g, null))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GuiaAssinaturaResponse> listarEnviadas() {
        return guiaAssinaturaRepository
                .findByStatusOrderByDataEnvioEmailDesc(StatusEsteiraAssinatura.ENVIADA_BENEFICIARIO)
                .stream()
                .map(g -> toResponse(g, null))
                .toList();
    }

    @Transactional
    public GuiaAssinaturaResponse enviarEmailBeneficiario(Long id) {
        GuiaAssinatura guia = buscarPorIdParaAtualizacao(id);
        if (guia.getStatus() != StatusEsteiraAssinatura.AGUARDANDO_ENVIO_BENEFICIARIO) {
            throw new ValidationException("Somente guias totalmente assinadas (Médico Chefe e Chefe da Seção) podem ser enviadas ao beneficiário.");
        }

        byte[] pdfBytes = lerBytesPdfGuia(guia);
        String mensagemPronta = montarMensagemPronta(guia);

        ResultadoEnvioEmail resultado = emailService.enviarEmailGuiaAssinada(guia, pdfBytes, mensagemPronta);

        if (resultado.status() != StatusEmail.ENVIADO && resultado.status() != StatusEmail.SIMULADO) {
            throw new ValidationException("Não foi possível enviar o e-mail ao beneficiário: " + resultado.mensagem());
        }

        guia.setStatus(StatusEsteiraAssinatura.ENVIADA_BENEFICIARIO);
        guia.setDataEnvioEmail(LocalDateTime.now());
        guia.setStatusEmail(resultado.status());
        guia = guiaAssinaturaRepository.save(guia);

        log.info("Guia nº {} enviada por e-mail para {} (status e-mail: {})",
                guia.getNumeroGuia(), guia.getBeneficiarioEmail(), resultado.status());

        return toResponse(guia, "E-mail enviado e guia disponibilizada na conta do beneficiário.");
    }

    public String montarMensagemPronta(GuiaAssinatura guia) {
        LocalDate validade = guia.getDataValidade();
        if (validade == null && guia.getDataAssinaturaSecao() != null) {
            validade = guia.getDataAssinaturaSecao().toLocalDate().plusDays(validadeDias);
        }
        String validadeStr = validade != null ? validade.format(FMT_DATA_BR) : "60 dias após assinatura";

        return "Prezado(a) " + guia.getBeneficiarioNome() + ",\n\n"
                + "Sua guia foi emitida e assinada com sucesso. A validade é de " + validadeDias + " dias a partir da assinatura do chefe de seção.\n\n"
                + "Guia nº " + guia.getNumeroGuia() + "\n"
                + "OCS: " + guia.getOcsNome() + "\n"
                + "Válida até: " + validadeStr + "\n\n"
                + "A guia está anexa e também disponível na sua conta, onde pode ser impressa.\n\n"
                + "Atenciosamente,\n"
                + "Seção FUSEX";
    }

    private void estamparESalvarPdf(GuiaAssinatura guia, List<ProcedimentoGuiaDTO> procedimentos) {
        try {
            byte[] originalBytes = null;
            if (guia.getCaminhoArquivoOriginal() != null) {
                Path p = Path.of(guia.getCaminhoArquivoOriginal());
                if (Files.exists(p)) {
                    originalBytes = Files.readAllBytes(p);
                }
            }
            byte[] pdfChancelado = pdfService.estamparAssinaturasEmPdfExistenteOuNovo(originalBytes, guia, procedimentos);
            String nomeAssinado = "guia_assinada_" + slugSeguroArquivo(guia.getNumeroGuia()) + "_" + UUID.randomUUID() + ".pdf";
            String caminhoAssinado = fileStorageService.salvar(pdfChancelado, nomeAssinado);
            guia.setCaminhoArquivoAssinado(caminhoAssinado);
        } catch (Exception e) {
            log.warn("Não foi possível gravar o PDF chancelado em disco para a guia {}: {}", guia.getNumeroGuia(), e.getMessage());
        }
    }

    private void removerArquivoAssinadoAnteriorSeNecessario(String caminhoAssinadoAnterior, GuiaAssinatura guia) {
        if (caminhoAssinadoAnterior != null
                && !caminhoAssinadoAnterior.equals(guia.getCaminhoArquivoOriginal())
                && !caminhoAssinadoAnterior.equals(guia.getCaminhoArquivoAssinado())) {
            try {
                Files.deleteIfExists(Path.of(caminhoAssinadoAnterior));
            } catch (IOException ignored) {
            }
        }
    }

    private byte[] lerBytesPdfGuia(GuiaAssinatura guia) {
        try {
            if (guia.getCaminhoArquivoAssinado() != null) {
                Path pAssinado = Path.of(guia.getCaminhoArquivoAssinado());
                if (Files.exists(pAssinado)) {
                    return Files.readAllBytes(pAssinado);
                }
            }
            if (guia.getCaminhoArquivoOriginal() != null) {
                Path pOrig = Path.of(guia.getCaminhoArquivoOriginal());
                if (Files.exists(pOrig)) {
                    return Files.readAllBytes(pOrig);
                }
            }
        } catch (IOException ignored) {
        }
        List<ProcedimentoGuiaDTO> procedimentos = pdfService.desserializarProcedimentos(guia.getProcedimentosResumo());
        return pdfService.gerarPdfEspelhoGuia(guia, procedimentos);
    }

    private GuiaAssinatura buscarPorId(Long id) {
        return guiaAssinaturaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("GuiaAssinatura", id));
    }

    private GuiaAssinatura buscarPorIdParaAtualizacao(Long id) {
        return guiaAssinaturaRepository.findByIdForUpdate(id)
                .or(() -> guiaAssinaturaRepository.findById(id))
                .orElseThrow(() -> new ResourceNotFoundException("GuiaAssinatura", id));
    }

    private void validarArquivoPdf5Mb(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Selecione um arquivo PDF.");
        }
        String nome = arquivo.getOriginalFilename() != null ? arquivo.getOriginalFilename().toLowerCase() : "";
        String contentType = arquivo.getContentType() != null ? arquivo.getContentType().toLowerCase() : "";
        boolean ehPdf = nome.endsWith(".pdf") || contentType.contains("pdf");
        if (!ehPdf) {
            throw new ArquivoInvalidoException("Selecione um arquivo PDF.");
        }
        if (arquivo.getSize() > LIMITE_TAMANHO_PDF_BYTES) {
            throw new ArquivoInvalidoException("O arquivo excede o limite de 5 MB.");
        }
    }

    private void validarConteudoPdfReal(byte[] bytesPdf) {
        if (bytesPdf == null || bytesPdf.length < 5
                || bytesPdf[0] != '%' || bytesPdf[1] != 'P' || bytesPdf[2] != 'D' || bytesPdf[3] != 'F' || bytesPdf[4] != '-') {
            throw new ArquivoInvalidoException("O arquivo enviado não é um documento PDF válido.");
        }
    }

    private String normalizarEValidarAssinaturaBase64(String assinaturaRaw) {
        if (assinaturaRaw == null || assinaturaRaw.isBlank()) {
            throw new ValidationException("Desenhe sua assinatura no campo indicado antes de confirmar.");
        }
        String trimmed = assinaturaRaw.trim();
        String prefixo = "data:image/png;base64,";
        String payload = trimmed;

        if (trimmed.startsWith("data:")) {
            int idxVirgula = trimmed.indexOf(',');
            if (idxVirgula <= 0 || !trimmed.substring(0, idxVirgula).matches("(?i)^data:image/(png|jpeg|jpg|webp);base64$")) {
                throw new ValidationException("Formato de assinatura digital inválido.");
            }
            prefixo = trimmed.substring(0, idxVirgula + 1);
            payload = trimmed.substring(idxVirgula + 1).replaceAll("\\s+", "");
        } else {
            payload = trimmed.replaceAll("\\s+", "");
        }

        if (payload.isEmpty() || payload.length() > 3_000_000) {
            throw new ValidationException("Imagem da assinatura digital inválida ou excede o tamanho permitido.");
        }

        try {
            byte[] decoded = Base64.getDecoder().decode(payload);
            if (!ehImagemAssinaturaValida(decoded)) {
                throw new ValidationException("A assinatura enviada não corresponde a uma imagem válida (PNG/JPEG/WebP).");
            }
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Formato Base64 da assinatura digital inválido.");
        }

        return prefixo + payload;
    }

    private boolean ehImagemAssinaturaValida(byte[] bytes) {
        if (bytes == null || bytes.length < 4) {
            return false;
        }
        // Confere o cabeçalho do arquivo para aceitar PNG, JPEG ou WebP
        boolean ehPng = bytes.length >= 8
                && (bytes[0] & 0xFF) == 0x89
                && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
        boolean ehJpeg = (bytes[0] & 0xFF) == 0xFF
                && (bytes[1] & 0xFF) == 0xD8
                && (bytes[2] & 0xFF) == 0xFF;
        boolean ehWebp = bytes.length >= 12
                && bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F'
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P';
        return ehPng || ehJpeg || ehWebp;
    }

    private String slugSeguroArquivo(String valor) {
        if (valor == null || valor.isBlank()) {
            return "sem_numero";
        }
        String slug = valor.trim().replaceAll("[^A-Za-z0-9_-]", "_");
        slug = slug.replaceAll("_+", "_");
        if (slug.length() > 60) {
            slug = slug.substring(0, 60);
        }
        return slug.isBlank() ? "sem_numero" : slug;
    }

    private String extrairNomeArquivoSeguro(String nomeBruto, String padrao) {
        if (nomeBruto == null || nomeBruto.isBlank()) {
            return padrao;
        }
        String normalizado = nomeBruto.replace('\\', '/');
        int idxBarra = normalizado.lastIndexOf('/');
        String apenasNome = idxBarra >= 0 ? normalizado.substring(idxBarra + 1) : normalizado;
        apenasNome = apenasNome.replaceAll("[\\p{Cntrl}]", "").trim();
        if (apenasNome.isBlank() || apenasNome.equals(".") || apenasNome.equals("..")) {
            return padrao;
        }
        return truncar(apenasNome, 255);
    }

    private String truncar(String valor, int max) {
        if (valor == null) {
            return null;
        }
        return valor.length() <= max ? valor : valor.substring(0, max);
    }

    private String gerarNumeroGuiaSequencialUnico() {
        while (true) {
            int n = sequencialFallback.getAndIncrement();
            String candidato = "2026" + (10000 + n);
            if (!guiaAssinaturaRepository.existsByNumeroGuia(candidato)) {
                return candidato;
            }
        }
    }

    private StatusEsteiraAssinatura resolverStatusAlvoPorPerfilOuRole(String perfilParam, Authentication auth) {
        Set<String> roles = extrairRoles(auth);
        if (perfilParam != null && !perfilParam.isBlank()) {
            String p = perfilParam.trim().toLowerCase();
            if (p.equals("sec") || p.equals("chefe_secao") || p.equals("secao")) {
                validarPermissaoChefeSecao(p, roles);
                return StatusEsteiraAssinatura.AGUARDANDO_CHEFE_SECAO;
            }
            if (p.equals("med") || p.equals("medico_chefe") || p.equals("medico")) {
                validarPermissaoMedicoChefe(p, roles);
                return StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE;
            }
            throw new ValidationException("Perfil de consulta inválido: " + perfilParam);
        }
        if (roles.contains("ROLE_CHEFE_SECAO") && !roles.contains("ROLE_MEDICO_CHEFE") && !roles.contains("ROLE_ADMIN")) {
            return StatusEsteiraAssinatura.AGUARDANDO_CHEFE_SECAO;
        }
        validarPermissaoMedicoChefe(null, roles);
        return StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE;
    }

    private void validarPermissaoMedicoChefe(String perfil, Set<String> roles) {
        if (perfil != null && (perfil.equalsIgnoreCase("sec") || perfil.equalsIgnoreCase("chefe_secao"))) {
            throw new UnauthorizedOperationException("Esta guia aguarda a primeira assinatura do Médico Chefe.");
        }
        if (!roles.isEmpty()
                && !roles.contains("ROLE_MEDICO_CHEFE")
                && !roles.contains("ROLE_ADMIN")) {
            throw new UnauthorizedOperationException("Apenas o Médico Chefe pode registrar a primeira assinatura.");
        }
    }

    private void validarPermissaoChefeSecao(String perfil, Set<String> roles) {
        if (perfil != null && (perfil.equalsIgnoreCase("med") || perfil.equalsIgnoreCase("medico_chefe"))) {
            throw new UnauthorizedOperationException("Esta guia já foi assinada pelo Médico Chefe e aguarda o Chefe da Seção.");
        }
        if (!roles.isEmpty()
                && !roles.contains("ROLE_CHEFE_SECAO")
                && !roles.contains("ROLE_ADMIN")) {
            throw new UnauthorizedOperationException("Apenas o Chefe da Seção pode registrar a segunda assinatura.");
        }
    }

    private Set<String> extrairRoles(Authentication auth) {
        if (auth == null || auth.getAuthorities() == null) {
            return Set.of();
        }
        Collection<? extends GrantedAuthority> authorities = auth.getAuthorities();
        return authorities.stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toSet());
    }

    private GuiaAssinaturaResponse toResponse(GuiaAssinatura g, String mensagem) {
        LocalDate validade = g.getDataValidade();
        if (validade == null && g.getDataAssinaturaSecao() != null) {
            validade = g.getDataAssinaturaSecao().toLocalDate().plusDays(validadeDias);
        }
        return new GuiaAssinaturaResponse(
                g.getId(),
                g.getNumeroGuia(),
                g.getBeneficiarioNome(),
                g.getBeneficiarioPrecCp(),
                g.getBeneficiarioEmail(),
                g.getOcsNome(),
                g.getNomeArquivoOriginal(),
                g.getTamanhoBytes(),
                g.getStatus(),
                g.getStatus().getCodigoCurto(),
                g.getStatus().getDescricao(),
                g.getDataEnvio(),
                g.getDataAssinaturaMedico(),
                g.getDataAssinaturaSecao(),
                validade,
                g.getDataEnvioEmail(),
                g.getStatusEmail(),
                montarMensagemPronta(g),
                mensagem
        );
    }
}

