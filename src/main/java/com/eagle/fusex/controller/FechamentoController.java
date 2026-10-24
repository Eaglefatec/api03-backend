package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.request.FechamentoRequest;
import com.eagle.fusex.model.dto.response.FechamentoResponse;
import com.eagle.fusex.service.FechamentoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/fechamentos")
public class FechamentoController {

    private final FechamentoService fechamentoService;

    public FechamentoController(FechamentoService fechamentoService) {
        this.fechamentoService = fechamentoService;
    }

    @PostMapping
    public ResponseEntity<FechamentoResponse> enviarFechamento(@Valid @RequestBody FechamentoRequest request) {
        FechamentoResponse response = fechamentoService.enviarFechamento(
                request.getCompetenciaAno(), request.getCompetenciaMes());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<FechamentoResponse>> listarFechamentos() {
        return ResponseEntity.ok(fechamentoService.listarFechamentos());
    }
}
