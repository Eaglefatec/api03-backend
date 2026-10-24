package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.request.PreencherTriagemRequest;
import com.eagle.fusex.model.dto.response.SolicitacaoPublicaResponse;
import com.eagle.fusex.model.dto.response.TriagemResponse;
import com.eagle.fusex.service.TriagemPreGuiaService;
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
@RequestMapping("/solicitacoes/publico/{token}")
public class SolicitacaoPublicoController {

    private final TriagemPreGuiaService service;

    public SolicitacaoPublicoController(TriagemPreGuiaService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<SolicitacaoPublicaResponse> buscarSolicitacao(@PathVariable String token) {
        SolicitacaoPublicaResponse response = service.buscarPorToken(token);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/triagem")
    public ResponseEntity<TriagemResponse> preencherTriagem(@PathVariable String token,
                                                             @Valid @RequestBody PreencherTriagemRequest request) {
        TriagemResponse response = service.preencherTriagem(token, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
