package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.ProcedimentoResponse;
import com.eagle.fusex.service.ProcedimentoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/procedimentos")
public class ProcedimentoController {

    private final ProcedimentoService procedimentoService;

    public ProcedimentoController(ProcedimentoService procedimentoService) {
        this.procedimentoService = procedimentoService;
    }

    @GetMapping
    public ResponseEntity<List<ProcedimentoResponse>> listarProcedimentos(
            @RequestParam(required = false) String busca) {
        return ResponseEntity.ok(procedimentoService.listarProcedimentos(busca));
    }
}
