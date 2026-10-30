package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.dto.response.FaturamentoFechamentoResponse;
import com.eagle.fusex.model.dto.response.FaturamentoLoteResponse;
import com.eagle.fusex.service.FaturamentoService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
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

    @GetMapping("/lotes/{loteId}/fatura")
    public ResponseEntity<byte[]> visualizarFatura(@PathVariable Long loteId) {
        return responderArquivo(faturamentoService.obterFatura(loteId));
    }

    @GetMapping("/lotes/{loteId}/espelho")
    public ResponseEntity<byte[]> visualizarEspelho(@PathVariable Long loteId) {
        return responderArquivo(faturamentoService.obterEspelho(loteId));
    }

    private ResponseEntity<byte[]> responderArquivo(ArquivoLoteDownload arquivo) {
        MediaType tipo = arquivo.tipo() != null
                ? MediaType.parseMediaType(arquivo.tipo())
                : MediaType.APPLICATION_OCTET_STREAM;
        ContentDisposition disposicao = ContentDisposition.inline()
                .filename(arquivo.nome(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .body(arquivo.conteudo());
    }
}
