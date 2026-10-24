package com.eagle.fusex.service;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.exception.OcsInvalidoException;
import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.exception.ValidationException;
import com.eagle.fusex.model.dto.response.LoteResponse;
import com.eagle.fusex.model.entity.Guia;
import com.eagle.fusex.model.entity.Lote;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.enums.StatusGuia;
import com.eagle.fusex.repository.GuiaRepository;
import com.eagle.fusex.repository.LoteRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.security.SecurityUtils;
import com.eagle.fusex.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class LoteService {

    private static final Logger log = LoggerFactory.getLogger(LoteService.class);

    private final LoteRepository loteRepository;
    private final GuiaRepository guiaRepository;
    private final OcsRepository ocsRepository;
    private final FileStorageService fileStorageService;

    public LoteService(LoteRepository loteRepository, GuiaRepository guiaRepository,
                        OcsRepository ocsRepository, FileStorageService fileStorageService) {
        this.loteRepository = loteRepository;
        this.guiaRepository = guiaRepository;
        this.ocsRepository = ocsRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public LoteResponse gerarLote(List<Long> guiaIds, MultipartFile fatura, MultipartFile espelho) {
        Ocs ocs = buscarOcsLogada();

        if (guiaIds == null || guiaIds.isEmpty()) {
            throw new ValidationException("Selecione ao menos uma guia para gerar o lote");
        }

        List<Guia> guias = guiaRepository.findAllById(guiaIds);
        if (guias.size() != guiaIds.size()) {
            throw new ResourceNotFoundException("Uma ou mais guias informadas não foram encontradas");
        }
        for (Guia guia : guias) {
            if (!guia.getOcs().getOcsId().equals(ocs.getOcsId())) {
                throw new ValidationException("A guia " + guia.getNumeroGuia() + " não pertence a esta OCS");
            }
            if (guia.getStatus() != StatusGuia.SEM_LOTE) {
                throw new ValidationException("A guia " + guia.getNumeroGuia() + " já está em um lote");
            }
        }

        validarArquivoFatura(fatura);
        validarArquivoEspelho(espelho);

        LocalDate hoje = LocalDate.now();
        int competenciaAno = hoje.getYear();
        int competenciaMes = hoje.getMonthValue();
        int numeroLote = (int) loteRepository.countByOcs_OcsIdAndCompetenciaAnoAndCompetenciaMes(
                ocs.getOcsId(), competenciaAno, competenciaMes) + 1;

        String faturaCaminho = fileStorageService.salvar(fatura, UUID.randomUUID() + "-" + extrairExtensao(fatura));
        String espelhoCaminho = fileStorageService.salvar(espelho, UUID.randomUUID() + "-" + extrairExtensao(espelho));

        Lote lote = new Lote();
        lote.setOcs(ocs);
        lote.setNumeroLote(numeroLote);
        lote.setCompetenciaAno(competenciaAno);
        lote.setCompetenciaMes(competenciaMes);
        lote.setDataGeracao(LocalDateTime.now());
        lote.setFaturaNomeArquivo(fatura.getOriginalFilename());
        lote.setFaturaCaminhoArquivo(faturaCaminho);
        lote.setFaturaTipoArquivo(fatura.getContentType());
        lote.setEspelhoNomeArquivo(espelho.getOriginalFilename());
        lote.setEspelhoCaminhoArquivo(espelhoCaminho);
        lote.setEspelhoTipoArquivo(espelho.getContentType());

        Lote salvo = loteRepository.save(lote);

        guias.forEach(guia -> {
            guia.setStatus(StatusGuia.EM_LOTE);
            guia.setLote(salvo);
        });
        guiaRepository.saveAll(guias);

        log.info("Lote {} gerado pela OCS {} com {} guias", salvo.getNumeroLote(), ocs.getOcsNome(), guias.size());

        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<LoteResponse> listarLotes() {
        Ocs ocs = buscarOcsLogada();
        return loteRepository.findByOcs_LoginUsuarioAndFechamentoIsNullOrderByDataGeracaoDesc(ocs.getLoginUsuario()).stream()
                .map(this::toResponse)
                .toList();
    }

    private Ocs buscarOcsLogada() {
        String loginUsuario = SecurityUtils.getAuthenticatedUsername()
                .orElseThrow(() -> new OcsInvalidoException("Usuário não autenticado"));
        return ocsRepository.findByLoginUsuario(loginUsuario)
                .orElseThrow(() -> new OcsInvalidoException("Nenhuma OCS associada a este usuário"));
    }

    private void validarArquivoFatura(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Anexe a fatura em formato PDF");
        }
        String nome = arquivo.getOriginalFilename();
        String nomeMinusculo = nome != null ? nome.toLowerCase() : "";
        if (!nomeMinusculo.endsWith(".pdf")) {
            throw new ArquivoInvalidoException("Anexe a fatura em formato PDF");
        }
        validarTamanho(arquivo);
    }

    private void validarArquivoEspelho(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Anexe o espelho em PDF, XLSX ou CSV");
        }
        String nome = arquivo.getOriginalFilename();
        String nomeMinusculo = nome != null ? nome.toLowerCase() : "";
        boolean extensaoValida = nomeMinusculo.endsWith(".pdf")
                || nomeMinusculo.endsWith(".xlsx")
                || nomeMinusculo.endsWith(".csv");
        if (!extensaoValida) {
            throw new ArquivoInvalidoException("Anexe o espelho em PDF, XLSX ou CSV");
        }
        validarTamanho(arquivo);
    }

    private void validarTamanho(MultipartFile arquivo) {
        if (arquivo.getSize() > 10 * 1024 * 1024) {
            throw new ArquivoInvalidoException("O arquivo deve ter até 10 MB");
        }
    }

    private String extrairExtensao(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        if (nome == null || !nome.contains(".")) {
            return "bin";
        }
        return nome.substring(nome.lastIndexOf(".") + 1).toLowerCase();
    }

    private LoteResponse toResponse(Lote lote) {
        long quantidadeGuias = guiaRepository.countByLoteId(lote.getId());
        return new LoteResponse(lote.getId(), lote.getNumeroLote(), lote.getCompetenciaAno(),
                lote.getCompetenciaMes(), lote.getDataGeracao(), lote.getFaturaNomeArquivo(),
                lote.getEspelhoNomeArquivo(), quantidadeGuias);
    }
}
