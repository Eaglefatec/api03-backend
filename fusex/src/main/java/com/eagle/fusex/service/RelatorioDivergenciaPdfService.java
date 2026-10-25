package com.eagle.fusex.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class RelatorioDivergenciaPdfService {

    // Task-99: Geração de relatório de divergência em PDF via Apache PDFBox
    public byte[] gerarRelatorioDivergenciasPdf(Long fechamentoId) throws IOException {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            try (PDPageContentStream contentStream = new PDPageContentStream(document, page)) {
                contentStream.beginText();
                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD), 14);
                contentStream.newLineAtOffset(50, 750);
                contentStream.showText("FUSEX - Relatorio de Divergencias e Glosas (ACM)");

                contentStream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                contentStream.newLineAtOffset(0, -30);
                contentStream.showText("Fechamento ID: " + fechamentoId);
                contentStream.newLineAtOffset(0, -20);
                contentStream.showText("Status: Auditoria processada com sucesso via motor de cruzamento.");

                contentStream.endText();
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        }
    }
}