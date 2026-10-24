package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.LoteResponse;
import com.eagle.fusex.service.LoteService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/lotes")
public class LoteController {

    private final LoteService loteService;

    public LoteController(LoteService loteService) {
        this.loteService = loteService;
    }

    @PostMapping
    public ResponseEntity<LoteResponse> gerarLote(@RequestParam("guiaIds") List<Long> guiaIds,
                                                    @RequestParam("fatura") MultipartFile fatura,
                                                    @RequestParam("espelho") MultipartFile espelho) {
        LoteResponse response = loteService.gerarLote(guiaIds, fatura, espelho);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<LoteResponse>> listarLotes() {
        return ResponseEntity.ok(loteService.listarLotes());
    }
}
