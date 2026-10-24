package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.request.CriarSolicitacaoRequest;
import com.eagle.fusex.model.dto.response.PreGuiaConsolidadaResponse;
import com.eagle.fusex.model.dto.response.SolicitacaoResponse;
import com.eagle.fusex.service.PreGuiaConsolidadaService;
import com.eagle.fusex.service.SolicitacaoMedicaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/solicitacoes")
public class SolicitacaoMedicoController {

    private final SolicitacaoMedicaService service;
    private final PreGuiaConsolidadaService preGuiaConsolidadaService;

    public SolicitacaoMedicoController(SolicitacaoMedicaService service,
                                       PreGuiaConsolidadaService preGuiaConsolidadaService) {
        this.service = service;
        this.preGuiaConsolidadaService = preGuiaConsolidadaService;
    }

    @PostMapping
    public ResponseEntity<SolicitacaoResponse> criarSolicitacao(@Valid @RequestBody CriarSolicitacaoRequest request) {
        SolicitacaoResponse response = service.criar(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{token}/pre-guia")
    public ResponseEntity<PreGuiaConsolidadaResponse> consultarPreGuia(@PathVariable String token) {
        PreGuiaConsolidadaResponse response = preGuiaConsolidadaService.consultar(token);
        return ResponseEntity.ok(response);
    }
}
