package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.MedicoResumoResponse;
import com.eagle.fusex.service.MedicoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/medicos")
public class MedicoController {

    private final MedicoService medicoService;

    public MedicoController(MedicoService medicoService) {
        this.medicoService = medicoService;
    }

    @GetMapping
    public ResponseEntity<List<MedicoResumoResponse>> listarMedicos() {
        return ResponseEntity.ok(medicoService.listarTodos());
    }
}
