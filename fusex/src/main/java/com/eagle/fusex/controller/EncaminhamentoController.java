package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.EncaminhamentoResponse;
import com.eagle.fusex.service.EncaminhamentoService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/encaminhamentos")
public class EncaminhamentoController {

    private final EncaminhamentoService encaminhamentoService;

    public EncaminhamentoController(EncaminhamentoService encaminhamentoService) {
        this.encaminhamentoService = encaminhamentoService;
    }

    @PostMapping
    public ResponseEntity<EncaminhamentoResponse> enviarEncaminhamento(
            @RequestParam(value = "medicoId", required = false) Long medicoId,
            @RequestParam("arquivo") MultipartFile arquivo) {

        Long resolvedMedicoId = medicoId != null ? medicoId : 1L;
        EncaminhamentoResponse response = encaminhamentoService.enviarEncaminhamento(resolvedMedicoId, arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
