package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.request.GlosaRequest;
import com.eagle.fusex.model.dto.response.DivergenciaResponse;
import com.eagle.fusex.model.dto.response.FaturamentoLoteResponse;
import com.eagle.fusex.model.entity.GuiaItem;
import com.eagle.fusex.service.LisuraService;
import com.eagle.fusex.service.RelatorioDivergenciaPdfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lisura")
public class LisuraController {

    private final LisuraService lisuraService;
    private final RelatorioDivergenciaPdfService pdfService;

    public LisuraController(LisuraService lisuraService,
                            RelatorioDivergenciaPdfService pdfService) {
        this.lisuraService = lisuraService;
        this.pdfService = pdfService;
    }

    @GetMapping("/fechamento/{fechamentoId}/lotes")
    public ResponseEntity<List<FaturamentoLoteResponse>> listarLotesPorFechamento(@PathVariable Long fechamentoId) {
        return ResponseEntity.ok(lisuraService.listarLotesPorFechamento(fechamentoId));
    }

    @GetMapping("/fechamentos")
    public ResponseEntity<List<Map<String, Object>>> listarFechamentos() {
        return ResponseEntity.ok(lisuraService.listarFechamentosFormatados());
    }

    @GetMapping("/cruzamento/{fechamentoId}")
    public ResponseEntity<List<DivergenciaResponse>> executarCruzamento(@PathVariable Long fechamentoId) {
        return ResponseEntity.ok(lisuraService.executarCruzamentoDados(fechamentoId));
    }

    @GetMapping("/guia-itens/{guiaId}")
    public ResponseEntity<List<GuiaItem>> listarItensDaGuia(@PathVariable Long guiaId) {
        return ResponseEntity.ok(lisuraService.listarItensDaGuia(guiaId));
    }

    @PostMapping("/glosas")
    public ResponseEntity<Void> registrarGlosa(@RequestBody @Valid GlosaRequest request) {
        lisuraService.registrarGlosa(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/relatorio/{fechamentoId}")
    public ResponseEntity<byte[]> baixarRelatorioPdf(@PathVariable Long fechamentoId) throws IOException {
        byte[] pdfBytes = pdfService.gerarRelatorioDivergenciasPdf(fechamentoId);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "relatorio_divergencias_" + fechamentoId + ".pdf");

        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }
}