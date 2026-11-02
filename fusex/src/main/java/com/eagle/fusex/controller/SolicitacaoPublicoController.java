package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.request.PreencherTriagemRequest;
import com.eagle.fusex.model.dto.response.OcsResponse;
import com.eagle.fusex.model.dto.response.SolicitacaoPublicaResponse;
import com.eagle.fusex.model.dto.response.TriagemResponse;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.service.OcsService;
import com.eagle.fusex.service.QrCodeService;
import com.eagle.fusex.service.TriagemPreGuiaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/solicitacoes/publico/{token}")
public class SolicitacaoPublicoController {

    private final TriagemPreGuiaService triagemService;
    private final QrCodeService qrCodeService;
    private final OcsService ocsService;

    public SolicitacaoPublicoController(TriagemPreGuiaService triagemService,
                                       QrCodeService qrCodeService,
                                       OcsService ocsService) {
        this.triagemService = triagemService;
        this.qrCodeService = qrCodeService;
        this.ocsService = ocsService;
    }

    @GetMapping
    public ResponseEntity<SolicitacaoPublicaResponse> buscarSolicitacao(@PathVariable String token) {
        SolicitacaoPublicaResponse response = triagemService.buscarPorToken(token);
        return ResponseEntity.ok(response);
    }

    @GetMapping(value = "/qrcode", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> gerarQrCode(@PathVariable String token) {
        triagemService.validarAcessoPublico(token);
        byte[] qrCodeBytes = qrCodeService.gerarQrCodePngPorToken(token);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .body(qrCodeBytes);
    }

    @GetMapping("/ocs-disponiveis")
    public ResponseEntity<List<OcsResponse>> listarOcsDisponiveis(@PathVariable String token) {
        SolicitacaoPublicaResponse solicitacao = triagemService.buscarPorToken(token);
        List<String> codigos = solicitacao.getProcedimentos().stream()
                .map(SolicitacaoPublicaResponse.ProcedimentoResumo::getCodigoDgp)
                .toList();

        List<OcsResponse> ocsDisponiveis = ocsService.listarPorProcedimentos(codigos);
        return ResponseEntity.ok(ocsDisponiveis);
    }

    @PostMapping("/triagem")
    public ResponseEntity<TriagemResponse> preencherTriagem(@PathVariable String token,
                                                             @Valid @RequestBody PreencherTriagemRequest request) {
        TriagemResponse response = triagemService.preencherTriagem(token, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
