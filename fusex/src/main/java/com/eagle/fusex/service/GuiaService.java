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

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class GuiaService {

    private static final Logger log = LoggerFactory.getLogger(GuiaService.class);

    private final GuiaRepository guiaRepository;
    private final OcsRepository ocsRepository;
    private final FileStorageService fileStorageService;

    public GuiaService(GuiaRepository guiaRepository, OcsRepository ocsRepository,
                        FileStorageService fileStorageService) {
        this.guiaRepository = guiaRepository;
        this.ocsRepository = ocsRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public GuiaResponse anexarGuia(String numeroGuia, String beneficiarioPrecCp, LocalDate dataAtendimento,
                                    MultipartFile arquivo) {
        Ocs ocs = buscarOcsLogada();
        validarArquivo(arquivo);
        validarNumeroGuia(numeroGuia);

        String nomeArquivoUnico = UUID.randomUUID() + ".pdf";
        String caminhoArquivo = fileStorageService.salvar(arquivo, nomeArquivoUnico);

        Guia guia = new Guia();
        guia.setOcs(ocs);
        guia.setNumeroGuia(numeroGuia);
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

    private void validarNumeroGuia(String numeroGuia) {
        if (guiaRepository.existsByNumeroGuia(numeroGuia)) {
            throw new ArquivoInvalidoException("Já existe uma guia anexada com este número");
        }
    }

    private GuiaResponse toResponse(Guia guia) {
        return new GuiaResponse(guia.getId(), guia.getNumeroGuia(), guia.getBeneficiarioPrecCp(),
                guia.getDataAtendimento(), guia.getNomeArquivo(), guia.getStatus());
    }
}
