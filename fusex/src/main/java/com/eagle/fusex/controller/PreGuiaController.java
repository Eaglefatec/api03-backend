package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.dto.response.GuiaAssinaturaResponse;
import com.eagle.fusex.model.dto.response.PreGuiaResumoResponse;
import com.eagle.fusex.model.enums.StatusPreGuia;
import com.eagle.fusex.service.PreGuiaService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/pre-guias")
public class PreGuiaController {

    private final PreGuiaService preGuiaService;

    public PreGuiaController(PreGuiaService preGuiaService) {
        this.preGuiaService = preGuiaService;
    }

    @GetMapping
    public ResponseEntity<List<PreGuiaResumoResponse>> listar(@RequestParam(required = false) StatusPreGuia status) {
        return ResponseEntity.ok(preGuiaService.listar(status));
    }

    @PostMapping("/{token}/confirmar-emissao")
    public ResponseEntity<PreGuiaResumoResponse> confirmarEmissao(@PathVariable String token) {
        return ResponseEntity.ok(preGuiaService.confirmarEmissao(token));
    }

    @PostMapping(value = "/{token}/guia-assinatura", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GuiaAssinaturaResponse> enviarParaAssinatura(
            @PathVariable String token,
            @RequestParam("arquivo") MultipartFile arquivo,
            @RequestParam(value = "confirmarCadastro", required = false, defaultValue = "false") boolean confirmarCadastro,
            @RequestParam(value = "pacienteNome", required = false) String pacienteNome,
            @RequestParam(value = "pacienteCpf", required = false) String pacienteCpf,
            @RequestParam(value = "pacientePrec", required = false) String pacientePrec,
            @RequestParam(value = "pacienteOm", required = false) String pacienteOm,
            @RequestParam(value = "pacienteEmail", required = false) String pacienteEmail,
            @RequestParam(value = "pacienteTelefone", required = false) String pacienteTelefone,
            @RequestParam(value = "pacienteIdade", required = false) Integer pacienteIdade) {
        if (confirmarCadastro) {
            com.eagle.fusex.model.dto.request.CadastroPacienteConfirmacaoDTO confirmacao =
                    new com.eagle.fusex.model.dto.request.CadastroPacienteConfirmacaoDTO(
                            true, pacienteNome, pacienteCpf, pacientePrec, pacienteOm,
                            pacienteEmail, pacienteTelefone, pacienteIdade);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(preGuiaService.enviarParaAssinatura(token, arquivo, confirmacao));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(preGuiaService.enviarParaAssinatura(token, arquivo));
    }

    @GetMapping("/{token}/encaminhamento")
    public ResponseEntity<byte[]> abrirEncaminhamento(@PathVariable String token) {
        ArquivoLoteDownload arquivo = preGuiaService.abrirEncaminhamento(token);
        MediaType tipo = resolverMediaTypeSeguro(arquivo.tipo());
        ContentDisposition disposicao = ContentDisposition.inline()
                .filename(arquivo.nome() != null ? arquivo.nome() : "encaminhamento", StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(arquivo.conteudo());
    }

    private MediaType resolverMediaTypeSeguro(String raw) {
        if (raw == null || raw.isBlank()) {
            return MediaType.APPLICATION_PDF;
        }
        String limpo = raw.trim().toLowerCase();
        if (limpo.equals("application/pdf")) return MediaType.APPLICATION_PDF;
        if (limpo.equals("image/png")) return MediaType.IMAGE_PNG;
        if (limpo.equals("image/jpeg") || limpo.equals("image/jpg")) return MediaType.IMAGE_JPEG;
        return MediaType.APPLICATION_OCTET_STREAM;
    }
}
