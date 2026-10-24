package com.eagle.fusex.service;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.exception.MedicoInvalidoException;
import com.eagle.fusex.model.dto.response.EncaminhamentoResponse;
import com.eagle.fusex.model.entity.Encaminhamento;
import com.eagle.fusex.model.entity.Medico;
import com.eagle.fusex.model.enums.StatusEncaminhamento;
import com.eagle.fusex.repository.EncaminhamentoRepository;
import com.eagle.fusex.repository.MedicoRepository;
import com.eagle.fusex.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class EncaminhamentoService {

    private static final Logger log = LoggerFactory.getLogger(EncaminhamentoService.class);

    private final MedicoRepository medicoRepository;
    private final EncaminhamentoRepository encaminhamentoRepository;
    private final FileStorageService fileStorageService;

    public EncaminhamentoService(MedicoRepository medicoRepository,
                                 EncaminhamentoRepository encaminhamentoRepository,
                                 FileStorageService fileStorageService) {
        this.medicoRepository = medicoRepository;
        this.encaminhamentoRepository = encaminhamentoRepository;
        this.fileStorageService = fileStorageService;
    }

    public EncaminhamentoResponse enviarEncaminhamento(Long medicoId, MultipartFile arquivo) {
        Medico medico = validarMedico(medicoId);
        validarArquivo(arquivo);

        LocalDateTime dataHora = LocalDateTime.now();
        String nomeArquivoOriginal = arquivo.getOriginalFilename();
        String extensao = extrairExtensao(nomeArquivoOriginal);
        String nomeArquivoUnico = UUID.randomUUID() + "." + extensao;

        String caminhoArquivo = fileStorageService.salvar(arquivo, nomeArquivoUnico);

        Encaminhamento encaminhamento = new Encaminhamento();
        encaminhamento.setMedico(medico);
        encaminhamento.setDataHora(dataHora);
        encaminhamento.setNomeArquivo(nomeArquivoOriginal);
        encaminhamento.setCaminhoArquivo(caminhoArquivo);
        encaminhamento.setTipoArquivo(arquivo.getContentType());
        encaminhamento.setTamanhoBytes(arquivo.getSize());
        encaminhamento.setStatus(StatusEncaminhamento.ENVIADO);

        Encaminhamento salvo = encaminhamentoRepository.save(encaminhamento);

        log.info("Encaminhamento salvo com sucesso. ID={}, médico={}", salvo.getId(), medico.getNome());

        return new EncaminhamentoResponse(
                salvo.getId(),
                salvo.getDataHora(),
                "Encaminhamento recebido com sucesso"
        );
    }

    private Medico validarMedico(Long medicoId) {
        return medicoRepository.findByIdAndCredenciadoTrue(medicoId)
                .orElseThrow(() -> new MedicoInvalidoException("Selecione um médico válido"));
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Carregue o encaminhamento em PDF, JPG ou PNG");
        }

        String nomeArquivo = arquivo.getOriginalFilename();
        long tamanho = arquivo.getSize();

        log.debug("Validando arquivo: nome={}, tamanho={}, contentType={}",
                nomeArquivo, tamanho, arquivo.getContentType());

        String nomeMinusculo = nomeArquivo != null ? nomeArquivo.toLowerCase() : "";
        boolean extensaoValida = nomeMinusculo.endsWith(".pdf") ||
                                nomeMinusculo.endsWith(".jpg") ||
                                nomeMinusculo.endsWith(".jpeg") ||
                                nomeMinusculo.endsWith(".png");

        log.debug("Extensão válida: {}", extensaoValida);

        if (!extensaoValida) {
            throw new ArquivoInvalidoException("Carregue o encaminhamento em PDF, JPG ou PNG");
        }

        if (tamanho > 6 * 1024 * 1024) {
            throw new ArquivoInvalidoException("Carregue o encaminhamento em PDF, JPG ou PNG");
        }
    }

    private String extrairExtensao(String nomeArquivo) {
        if (nomeArquivo == null || !nomeArquivo.contains(".")) {
            return "bin";
        }
        return nomeArquivo.substring(nomeArquivo.lastIndexOf(".") + 1).toLowerCase();
    }
}
