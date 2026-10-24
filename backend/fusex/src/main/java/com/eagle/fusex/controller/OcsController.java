package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.OcsResponse;
import com.eagle.fusex.model.enums.Especialidade;
import com.eagle.fusex.service.OcsService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/ocs")
public class OcsController {

    private final OcsService ocsService;

    public OcsController(OcsService ocsService) {
        this.ocsService = ocsService;
    }

    @GetMapping(params = "procedimentoId")
    public ResponseEntity<List<OcsResponse>> listarOcsPorProcedimento(@RequestParam String procedimentoId) {
        return ResponseEntity.ok(ocsService.listarPorProcedimento(procedimentoId));
    }

    @GetMapping(params = "especialidades")
    public ResponseEntity<List<OcsResponse>> listarOcsPorEspecialidades(@RequestParam Set<Especialidade> especialidades) {
        return ResponseEntity.ok(ocsService.listarPorEspecialidades(especialidades));
    }

    @GetMapping
    public ResponseEntity<List<OcsResponse>> listarTodasOcs() {
        return ResponseEntity.ok(ocsService.listarTodas());
    }
}
