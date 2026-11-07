package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.request.DecisaoAssinaturaRequest;
import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.dto.response.GuiaAssinaturaDetalheResponse;
import com.eagle.fusex.model.dto.response.GuiaAssinaturaResponse;
import com.eagle.fusex.model.dto.response.PainelAssinaturaResumoResponse;
import com.eagle.fusex.service.GuiaAssinaturaService;
import jakarta.validation.Valid;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/assinaturas")
public class GuiaAssinaturaController {

    private final GuiaAssinaturaService guiaAssinaturaService;

    public GuiaAssinaturaController(GuiaAssinaturaService guiaAssinaturaService) {
        this.guiaAssinaturaService = guiaAssinaturaService;
    }

    @PostMapping(value = "/guias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<GuiaAssinaturaResponse> enviarGuia(@RequestParam("arquivo") MultipartFile arquivo) {
        GuiaAssinaturaResponse response = guiaAssinaturaService.enviarGuiaParaAssinatura(arquivo);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/guias")
    public ResponseEntity<List<GuiaAssinaturaResponse>> listarGuias() {
        return ResponseEntity.ok(guiaAssinaturaService.listarGuiasAcompanhamento());
    }

    @GetMapping("/painel")
    public ResponseEntity<PainelAssinaturaResumoResponse> obterResumoPainel() {
        return ResponseEntity.ok(guiaAssinaturaService.obterResumoPainel());
    }

    @GetMapping({"/pendencias", "/guias/pendencias"})
    public ResponseEntity<List<GuiaAssinaturaResponse>> listarPendencias(
            @RequestParam(value = "perfil", required = false) String perfil,
            Authentication authentication) {
        return ResponseEntity.ok(guiaAssinaturaService.listarPendencias(perfil, authentication));
    }

    @GetMapping({"/guias/{id:\\d+}", "/guias/{id:\\d+}/detalhes"})
    public ResponseEntity<GuiaAssinaturaDetalheResponse> obterDetalhes(@PathVariable Long id) {
        return ResponseEntity.ok(guiaAssinaturaService.obterDetalhes(id));
    }

    @GetMapping("/guias/{id:\\d+}/pdf")
    public ResponseEntity<byte[]> visualizarPdf(@PathVariable Long id) {
        ArquivoLoteDownload arquivo = guiaAssinaturaService.obterArquivoPdf(id);
        ContentDisposition disposicao = ContentDisposition.inline()
                .filename(arquivo.nome(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposicao.toString())
                .body(arquivo.conteudo());
    }

    @PostMapping({"/guias/{id:\\d+}/decisao", "/guias/{id:\\d+}/assinar"})
    public ResponseEntity<GuiaAssinaturaResponse> processarDecisao(
            @PathVariable Long id,
            @Valid @RequestBody DecisaoAssinaturaRequest request,
            Authentication authentication) {
        return ResponseEntity.ok(guiaAssinaturaService.processarDecisao(id, request, authentication));
    }

    @GetMapping({"/prontas-envio", "/guias/prontas-envio"})
    public ResponseEntity<List<GuiaAssinaturaResponse>> listarProntasParaEnvio() {
        return ResponseEntity.ok(guiaAssinaturaService.listarProntasParaEnvio());
    }

    @GetMapping({"/enviadas", "/guias/enviadas"})
    public ResponseEntity<List<GuiaAssinaturaResponse>> listarEnviadas() {
        return ResponseEntity.ok(guiaAssinaturaService.listarEnviadas());
    }

    @PostMapping("/guias/{id:\\d+}/enviar-email")
    public ResponseEntity<GuiaAssinaturaResponse> enviarEmail(@PathVariable Long id) {
        return ResponseEntity.ok(guiaAssinaturaService.enviarEmailBeneficiario(id));
    }
}

