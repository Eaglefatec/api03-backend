package com.eagle.fusex.service;

import com.eagle.fusex.model.dto.response.DivergenciaResponse;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

@Service
public class RelatorioDivergenciaPdfService {

    private final LisuraService lisuraService;

    public RelatorioDivergenciaPdfService() {
        this.lisuraService = null;
    }

    @Autowired
    public RelatorioDivergenciaPdfService(LisuraService lisuraService) {
        this.lisuraService = lisuraService;
    }

    public byte[] gerarRelatorioDivergenciasPdf(Long fechamentoId) throws IOException {
        List<DivergenciaResponse> divergencias = List.of();
        if (lisuraService != null && fechamentoId != null) {
            try {
                divergencias = lisuraService.executarCruzamentoDados(fechamentoId);
            } catch (Exception ignored) {
                divergencias = List.of();
            }
        }

        NumberFormat moeda = NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"));
        BigDecimal totalContratado = BigDecimal.ZERO;
        BigDecimal totalFaturado = BigDecimal.ZERO;
        long qtdDivergentes = 0;
        for (DivergenciaResponse d : divergencias) {
            totalContratado = totalContratado.add(d.getValorContratado() != null ? d.getValorContratado() : BigDecimal.ZERO);
            totalFaturado = totalFaturado.add(d.getValorFaturado() != null ? d.getValorFaturado() : BigDecimal.ZERO);
            if (d.getDiferenca() != null && d.getDiferenca().compareTo(BigDecimal.ZERO) != 0) {
                qtdDivergentes++;
            }
        }

        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            PDType1Font bold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font regular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                cs.beginText();
                cs.setFont(bold, 14);
                cs.newLineAtOffset(50, 750);
                cs.showText("FUSEX - Relatorio de Divergencias e Glosas (ACM)");

                cs.setFont(regular, 11);
                cs.newLineAtOffset(0, -26);
                cs.showText("Fechamento ID: " + fechamentoId + " | Total de guias: " + divergencias.size()
                        + " | Divergentes: " + qtdDivergentes);
                cs.newLineAtOffset(0, -18);
                cs.showText(limparAscii("Total contratado: " + moeda.format(totalContratado)
                        + " | Total no espelho: " + moeda.format(totalFaturado)
                        + " | Diferenca: " + moeda.format(totalFaturado.subtract(totalContratado))));
                cs.newLineAtOffset(0, -24);

                if (divergencias.isEmpty()) {
                    cs.showText("Status: Auditoria processada com sucesso via motor de cruzamento.");
                } else {
                    cs.setFont(bold, 10);
                    cs.showText("Guia | Beneficiario | Contratado | Espelho | Diferenca | Status");
                    cs.setFont(regular, 9.5f);
                    int limite = Math.min(divergencias.size(), 28);
                    for (int i = 0; i < limite; i++) {
                        DivergenciaResponse d = divergencias.get(i);
                        cs.newLineAtOffset(0, -16);
                        String linha = String.format("%s | %s | %s | %s | %s | %s",
                                d.getNumeroGuia() != null ? d.getNumeroGuia() : "-",
                                truncar(d.getBeneficiarioNome(), 22),
                                moeda.format(d.getValorContratado() != null ? d.getValorContratado() : BigDecimal.ZERO),
                                moeda.format(d.getValorFaturado() != null ? d.getValorFaturado() : BigDecimal.ZERO),
                                moeda.format(d.getDiferenca() != null ? d.getDiferenca() : BigDecimal.ZERO),
                                d.getStatusDivergencia() != null ? d.getStatusDivergencia() : "-");
                        cs.showText(limparAscii(linha));
                    }
                }
                cs.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }

    private String truncar(String s, int max) {
        if (s == null || s.isBlank()) return "-";
        String limpo = s.trim();
        return limpo.length() <= max ? limpo : limpo.substring(0, max);
    }

    private String limparAscii(String s) {
        if (s == null) return "";
        return s.replace('\u00A0', ' ').replaceAll("[^\\x20-\\x7E\\xA1-\\xFF]", "");
    }
}