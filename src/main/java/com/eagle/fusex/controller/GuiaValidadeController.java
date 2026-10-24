package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.GuiaValidadeResponse;
import com.eagle.fusex.service.GuiaValidadeService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/guias")
public class GuiaValidadeController {

    private final GuiaValidadeService guiaValidadeService;

    public GuiaValidadeController(GuiaValidadeService guiaValidadeService) {
        this.guiaValidadeService = guiaValidadeService;
    }

    @GetMapping("/{numeroGuia}/validade")
    public ResponseEntity<GuiaValidadeResponse> consultarValidade(@PathVariable String numeroGuia) {
        return ResponseEntity.ok(guiaValidadeService.consultarValidade(numeroGuia));
    }
}
