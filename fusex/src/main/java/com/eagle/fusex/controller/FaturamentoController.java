package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.FaturamentoFechamentoResponse;
import com.eagle.fusex.model.dto.response.FaturamentoLoteResponse;
import com.eagle.fusex.service.FaturamentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/faturamento")
public class FaturamentoController {

    private final FaturamentoService faturamentoService;

    public FaturamentoController(FaturamentoService faturamentoService) {
        this.faturamentoService = faturamentoService;
    }

    @GetMapping("/fechamentos")
    public ResponseEntity<List<FaturamentoFechamentoResponse>> listarFechamentos() {
        return ResponseEntity.ok(faturamentoService.listarFechamentosPendentes());
    }

    @GetMapping("/fechamentos/{fechamentoId}/lotes")
    public ResponseEntity<List<FaturamentoLoteResponse>> listarLotes(@PathVariable Long fechamentoId) {
        return ResponseEntity.ok(faturamentoService.listarLotes(fechamentoId));
    }
}
