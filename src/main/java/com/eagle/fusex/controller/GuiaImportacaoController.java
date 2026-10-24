package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.request.CorrigirNumeroGuiaRequest;
import com.eagle.fusex.model.dto.response.GuiaImportacaoResponse;
import com.eagle.fusex.service.GuiaImportacaoService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/solicitacoes/{token}/guia-pdf")
public class GuiaImportacaoController {

    private final GuiaImportacaoService guiaImportacaoService;

    public GuiaImportacaoController(GuiaImportacaoService guiaImportacaoService) {
        this.guiaImportacaoService = guiaImportacaoService;
    }

    @PostMapping
    public ResponseEntity<GuiaImportacaoResponse> importarPdf(@PathVariable String token,
                                                               @RequestParam("arquivo") MultipartFile arquivo) {
        GuiaImportacaoResponse response = guiaImportacaoService.importarPdf(token, arquivo);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/numero")
    public ResponseEntity<GuiaImportacaoResponse> corrigirNumeroGuia(@PathVariable String token,
                                                                      @Valid @RequestBody CorrigirNumeroGuiaRequest request) {
        GuiaImportacaoResponse response = guiaImportacaoService.corrigirNumeroGuia(token, request.getNumeroGuia());
        return ResponseEntity.ok(response);
    }
}
