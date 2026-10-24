package com.eagle.fusex.service;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.exception.SolicitacaoNaoEncontradaException;
import com.eagle.fusex.model.dto.response.GuiaImportacaoResponse;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.storage.FileStorageService;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GuiaImportacaoService {

    private static final Logger log = LoggerFactory.getLogger(GuiaImportacaoService.class);

    private final SolicitacaoMedicaRepository solicitacaoRepository;
    private final FileStorageService fileStorageService;
    private final Pattern numeroGuiaPattern;
    private final int validadeDias;

    public GuiaImportacaoService(SolicitacaoMedicaRepository solicitacaoRepository,
                                 FileStorageService fileStorageService,
                                 @Value("${fusex.guia.numero-regex}") String numeroRegex,
                                 @Value("${fusex.guia.validade-dias}") int validadeDias) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.fileStorageService = fileStorageService;
        this.numeroGuiaPattern = Pattern.compile(numeroRegex);
        this.validadeDias = validadeDias;
    }

    @Transactional
    public GuiaImportacaoResponse importarPdf(String token, MultipartFile arquivo) {
        SolicitacaoMedica solicitacao = buscarSolicitacao(token);
        validarArquivo(arquivo);

        String numeroExtraido = extrairNumeroGuia(arquivo);
        String nomeArquivoUnico = UUID.randomUUID() + ".pdf";
        String caminhoArquivo = fileStorageService.salvar(arquivo, nomeArquivoUnico);

        LocalDateTime agora = LocalDateTime.now();
        solicitacao.setNumeroGuia(numeroExtraido);
        solicitacao.setDataImportacaoGuia(agora);
        solicitacao.setDataValidade(agora.toLocalDate().plusDays(validadeDias));
        solicitacao.setCaminhoArquivoGuia(caminhoArquivo);
        solicitacaoRepository.save(solicitacao);

        log.info("Guia importada para a solicitação {}. Número identificado: {}", solicitacao.getId(), numeroExtraido);

        String mensagem = numeroExtraido != null
                ? "Guia importada e número identificado automaticamente"
                : "Guia importada, mas o número não foi identificado automaticamente. Corrija manualmente.";

        return new GuiaImportacaoResponse(numeroExtraido, agora, solicitacao.getDataValidade(),
                numeroExtraido != null, mensagem);
    }

    @Transactional
    public GuiaImportacaoResponse corrigirNumeroGuia(String token, String numeroGuia) {
        SolicitacaoMedica solicitacao = buscarSolicitacao(token);
        solicitacao.setNumeroGuia(numeroGuia);
        solicitacaoRepository.save(solicitacao);

        return new GuiaImportacaoResponse(numeroGuia, solicitacao.getDataImportacaoGuia(),
                solicitacao.getDataValidade(), true, "Número da guia atualizado com sucesso");
    }

    private String extrairNumeroGuia(MultipartFile arquivo) {
        try (PDDocument documento = Loader.loadPDF(arquivo.getBytes())) {
            String texto = new PDFTextStripper().getText(documento).replace(' ', ' ');
            Matcher matcher = numeroGuiaPattern.matcher(texto);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
            return null;
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler o conteúdo do PDF");
        }
    }

    private SolicitacaoMedica buscarSolicitacao(String token) {
        return solicitacaoRepository.findByTokenPublico(token)
                .orElseThrow(() -> new SolicitacaoNaoEncontradaException("Solicitação não encontrada ou inválida"));
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Envie a guia em formato PDF");
        }

        String nomeArquivo = arquivo.getOriginalFilename();
        String nomeMinusculo = nomeArquivo != null ? nomeArquivo.toLowerCase() : "";
        if (!nomeMinusculo.endsWith(".pdf")) {
            throw new ArquivoInvalidoException("Envie a guia em formato PDF");
        }

        if (arquivo.getSize() > 10 * 1024 * 1024) {
            throw new ArquivoInvalidoException("O arquivo da guia deve ter até 10 MB");
        }
    }
}
