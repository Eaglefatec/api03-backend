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
    public ResponseEntity<GuiaAssinaturaResponse> enviarParaAssinatura(@PathVariable String token,
                                                                       @RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.status(HttpStatus.CREATED).body(preGuiaService.enviarParaAssinatura(token, arquivo));
    }

    @GetMapping("/{token}/encaminhamento")
    public ResponseEntity<byte[]> abrirEncaminhamento(@PathVariable String token) {
        ArquivoLoteDownload arquivo = preGuiaService.abrirEncaminhamento(token);
        MediaType tipo = arquivo.tipo() != null ? MediaType.parseMediaType(arquivo.tipo()) : MediaType.APPLICATION_PDF;
        ContentDisposition disposicao = ContentDisposition.inline()
                .filename(arquivo.nome(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .body(arquivo.conteudo());
    }
}
