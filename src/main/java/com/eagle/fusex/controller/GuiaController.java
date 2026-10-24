package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.GuiaResponse;
import com.eagle.fusex.service.GuiaService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/guias")
public class GuiaController {

    private final GuiaService guiaService;

    public GuiaController(GuiaService guiaService) {
        this.guiaService = guiaService;
    }

    @PostMapping
    public ResponseEntity<GuiaResponse> anexarGuia(@RequestParam("numeroGuia") String numeroGuia,
                                                     @RequestParam("beneficiarioPrecCp") String beneficiarioPrecCp,
                                                     @RequestParam("dataAtendimento") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dataAtendimento,
                                                     @RequestParam("arquivo") MultipartFile arquivo) {
        GuiaResponse response = guiaService.anexarGuia(numeroGuia, beneficiarioPrecCp, dataAtendimento, arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<GuiaResponse>> listarGuias() {
        return ResponseEntity.ok(guiaService.listarGuias());
    }
}
