package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.request.GlosaRequest;
import com.eagle.fusex.model.dto.response.DivergenciaResponse;
import com.eagle.fusex.model.entity.Fechamento;
import com.eagle.fusex.model.entity.GuiaItem;
import com.eagle.fusex.model.entity.Lote;
import com.eagle.fusex.repository.FechamentoRepository;
import com.eagle.fusex.repository.GuiaItemRepository;
import com.eagle.fusex.repository.LoteRepository;
import com.eagle.fusex.service.LisuraService;
import com.eagle.fusex.service.RelatorioDivergenciaPdfService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/lisura")
public class LisuraController {

    private final LisuraService lisuraService;
    private final RelatorioDivergenciaPdfService pdfService;
    private final FechamentoRepository fechamentoRepository;
    private final GuiaItemRepository guiaItemRepository;
    private final LoteRepository loteRepository; // Adicionado aqui

    public LisuraController(LisuraService lisuraService,
                            RelatorioDivergenciaPdfService pdfService,
                            FechamentoRepository fechamentoRepository,
                            GuiaItemRepository guiaItemRepository,
                            LoteRepository loteRepository) { // Adicionado no construtor
        this.lisuraService = lisuraService;
        this.pdfService = pdfService;
        this.fechamentoRepository = fechamentoRepository;
        this.guiaItemRepository = guiaItemRepository;
        this.loteRepository = loteRepository;
    }

    @GetMapping("/fechamento/{fechamentoId}/lotes")
    public ResponseEntity<List<Lote>> listarLotesPorFechamento(@PathVariable Long fechamentoId) {
        List<Lote> lotes = loteRepository.findByFechamentoId(fechamentoId);
        return ResponseEntity.ok(lotes);
    }

    @GetMapping("/fechamentos")
    @Transactional(readOnly = true)
    public ResponseEntity<List<Map<String, Object>>> listarFechamentos() {
        List<Fechamento> lista = fechamentoRepository.findAll();
        List<Map<String, Object>> resposta = new ArrayList<>();

        for (Fechamento f : lista) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("competenciaMes", f.getCompetenciaMes());
            map.put("competenciaAno", f.getCompetenciaAno());
            map.put("dataEnvio", f.getDataEnvio());
            map.put("status", f.getStatus());

            Map<String, Object> ocsMap = new HashMap<>();
            if (f.getOcs() != null) {
                ocsMap.put("ocsId", f.getOcs().getOcsId());
                ocsMap.put("ocsNome", f.getOcs().getOcsNome());
            }
            map.put("ocs", ocsMap);

            resposta.add(map);
        }

        return ResponseEntity.ok(resposta);
    }

    @GetMapping("/cruzamento/{fechamentoId}")
    public ResponseEntity<List<DivergenciaResponse>> executarCruzamento(@PathVariable Long fechamentoId) {
        return ResponseEntity.ok(lisuraService.executarCruzamentoDados(fechamentoId));
    }

    @GetMapping("/guia-itens/{guiaId}")
    public ResponseEntity<List<GuiaItem>> listarItensDaGuia(@PathVariable Long guiaId) {
        return ResponseEntity.ok(guiaItemRepository.findByGuiaId(guiaId));
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