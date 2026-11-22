package com.eagle.fusex.service;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.exception.OcsInvalidoException;
import com.eagle.fusex.model.dto.response.GuiaResponse;
import com.eagle.fusex.model.entity.Guia;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.enums.StatusGuia;
import com.eagle.fusex.repository.GuiaRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.security.SecurityUtils;
import com.eagle.fusex.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class GuiaService {

    private static final Logger log = LoggerFactory.getLogger(GuiaService.class);

    private final GuiaRepository guiaRepository;
    private final OcsRepository ocsRepository;
    private final FileStorageService fileStorageService;
    private final GuiaAssinaturaPdfService pdfService;

    public GuiaService(GuiaRepository guiaRepository, OcsRepository ocsRepository,
                       FileStorageService fileStorageService, GuiaAssinaturaPdfService pdfService) {
        this.guiaRepository = guiaRepository;
        this.ocsRepository = ocsRepository;
        this.fileStorageService = fileStorageService;
        this.pdfService = pdfService;
    }

    @Transactional
    public GuiaResponse anexarGuia(String numeroGuia, String beneficiarioPrecCp, LocalDate dataAtendimento,
                                   MultipartFile arquivo) {
        Ocs ocs = buscarOcsLogada();
        validarArquivo(arquivo);
        String numeroGuiaValidado = validarNumeroGuia(numeroGuia, arquivo);

        String nomeArquivoUnico = UUID.randomUUID() + ".pdf";
        String caminhoArquivo = fileStorageService.salvar(arquivo, nomeArquivoUnico);

        Guia guia = new Guia();
        guia.setOcs(ocs);
        guia.setNumeroGuia(numeroGuiaValidado);
        guia.setBeneficiarioPrecCp(beneficiarioPrecCp);
        guia.setDataAtendimento(dataAtendimento);
        guia.setNomeArquivo(arquivo.getOriginalFilename());
        guia.setCaminhoArquivo(caminhoArquivo);
        guia.setTipoArquivo(arquivo.getContentType());
        guia.setTamanhoBytes(arquivo.getSize());
        guia.setStatus(StatusGuia.SEM_LOTE);

        Guia salva = guiaRepository.save(guia);

        log.info("Guia {} anexada pela OCS {}", salva.getNumeroGuia(), ocs.getOcsNome());

        return toResponse(salva);
    }

    @Transactional(readOnly = true)
    public List<GuiaResponse> listarGuias() {
        Ocs ocs = buscarOcsLogada();
        return guiaRepository.findByOcs_LoginUsuarioOrderByCreatedAtDesc(ocs.getLoginUsuario()).stream()
                .map(this::toResponse)
                .toList();
    }

    private Ocs buscarOcsLogada() {
        String loginUsuario = SecurityUtils.getAuthenticatedUsername()
                .orElseThrow(() -> new OcsInvalidoException("Usuário não autenticado"));
        return ocsRepository.findByLoginUsuario(loginUsuario)
                .orElseThrow(() -> new OcsInvalidoException("Nenhuma OCS associada a este usuário"));
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Envie a guia assinada em formato PDF");
        }

        String nomeArquivo = arquivo.getOriginalFilename();
        String nomeMinusculo = nomeArquivo != null ? nomeArquivo.toLowerCase() : "";
        if (!nomeMinusculo.endsWith(".pdf")) {
            throw new ArquivoInvalidoException("Envie a guia assinada em formato PDF");
        }

        if (arquivo.getSize() > 10 * 1024 * 1024) {
            throw new ArquivoInvalidoException("O arquivo da guia deve ter até 10 MB");
        }
    }

    /**
     * Valida o número da guia usando o número que está escrito dentro do PDF (e não o nome do arquivo).
     * Se o PDF trouxer um número, ele precisa bater com o informado no formulário e não pode já existir.
     * Se não for possível ler o número no PDF, valida apenas o número informado no formulário.
     *
     * @return o número da guia que deve ser gravado
     */
    private String validarNumeroGuia(String numeroInformado, MultipartFile arquivo) {
        String numeroDigitado = normalizarNumeroGuia(numeroInformado);
        String numeroNoPdf = lerNumeroGuiaDoPdf(arquivo);

        if (numeroNoPdf != null && !numeroNoPdf.equalsIgnoreCase(numeroDigitado)) {
            throw new ArquivoInvalidoException("O número informado (" + numeroDigitado
                    + ") não confere com o número da guia identificado no PDF (" + numeroNoPdf + ")");
        }

        String numeroFinal = numeroNoPdf != null ? numeroNoPdf : numeroDigitado;
        if (guiaRepository.existsByNumeroGuia(numeroFinal)) {
            throw new ArquivoInvalidoException("Já existe uma guia anexada com este número (" + numeroFinal + ")");
        }
        return numeroFinal;
    }

    private String lerNumeroGuiaDoPdf(MultipartFile arquivo) {
        try {
            GuiaAssinaturaPdfService.DadosExtraidosPdf dados = pdfService.extrairDadosDoPdf(arquivo.getBytes());
            if (dados == null) {
                return null;
            }
            String numero = normalizarNumeroGuia(dados.numeroGuia());
            return numero.isEmpty() ? null : numero;
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler o conteúdo do PDF");
        }
    }

    private String normalizarNumeroGuia(String numero) {
        if (numero == null) {
            return "";
        }
        return numero.trim().replaceAll("[./-]+$", "");
    }

    private GuiaResponse toResponse(Guia guia) {
        return new GuiaResponse(guia.getId(), guia.getNumeroGuia(), guia.getBeneficiarioPrecCp(),
                guia.getDataAtendimento(), guia.getNomeArquivo(), guia.getStatus());
    }
}