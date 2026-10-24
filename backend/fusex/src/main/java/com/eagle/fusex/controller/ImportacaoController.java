package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.ImportacaoResumoResponse;
import com.eagle.fusex.service.importacao.OcsCadastroImportacaoService;
import com.eagle.fusex.service.importacao.OcsPrecoImportacaoService;
import com.eagle.fusex.service.importacao.TussImportacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/importacao")
public class ImportacaoController {

    private final TussImportacaoService tussImportacaoService;
    private final OcsPrecoImportacaoService ocsPrecoImportacaoService;
    private final OcsCadastroImportacaoService ocsCadastroImportacaoService;

    public ImportacaoController(TussImportacaoService tussImportacaoService,
                                OcsPrecoImportacaoService ocsPrecoImportacaoService,
                                OcsCadastroImportacaoService ocsCadastroImportacaoService) {
        this.tussImportacaoService = tussImportacaoService;
        this.ocsPrecoImportacaoService = ocsPrecoImportacaoService;
        this.ocsCadastroImportacaoService = ocsCadastroImportacaoService;
    }

    @PostMapping("/tuss")
    public ResponseEntity<ImportacaoResumoResponse> importarTuss(@RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(tussImportacaoService.importar(arquivo));
    }

    @PostMapping("/ocs-precos")
    public ResponseEntity<ImportacaoResumoResponse> importarOcsPrecos(@RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(ocsPrecoImportacaoService.importar(arquivo));
    }

    @PostMapping("/ocs-cadastro")
    public ResponseEntity<ImportacaoResumoResponse> importarOcsCadastro(@RequestParam("arquivo") MultipartFile arquivo) {
        return ResponseEntity.ok(ocsCadastroImportacaoService.importar(arquivo));
    }
}
