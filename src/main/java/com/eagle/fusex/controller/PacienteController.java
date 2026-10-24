package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.request.CadastrarPacienteRequest;
import com.eagle.fusex.model.dto.response.PacienteResponse;
import com.eagle.fusex.service.PacienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/pacientes")
public class PacienteController {

    private final PacienteService pacienteService;

    public PacienteController(PacienteService pacienteService) {
        this.pacienteService = pacienteService;
    }

    @GetMapping
    public ResponseEntity<List<PacienteResponse>> buscarPacientes(@RequestParam(required = false) String busca) {
        return ResponseEntity.ok(pacienteService.buscarPacientes(busca));
    }

    @PostMapping
    public ResponseEntity<PacienteResponse> cadastrarPaciente(@Valid @RequestBody CadastrarPacienteRequest request) {
        PacienteResponse response = pacienteService.cadastrarPaciente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
