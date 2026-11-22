package com.eagle.fusex.service;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class GuiaValorContratadoPdfServiceTest {

    private final GuiaValorContratadoPdfService service = new GuiaValorContratadoPdfService();

    static byte[] pdfComLinhas(String... linhas) throws IOException {
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            try (PDPageContentStream conteudo = new PDPageContentStream(documento, pagina)) {
                conteudo.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 9);
                conteudo.beginText();
                conteudo.setLeading(12);
                conteudo.newLineAtOffset(40, 800);
                for (String linha : linhas) {
                    conteudo.showText(linha);
                    conteudo.newLine();
                }
                conteudo.endText();
            }
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            documento.save(saida);
            return saida.toByteArray();
        }
    }

    @Test
    void testExtrair_GuiaDeEncaminhamento_UsaTotalDaGuia() throws IOException {
        byte[] pdf = pdfComLinhas(
                "GUIA DE ENCAMINHAMENTO Nr: 202610859 / FUSEX",
                "Procedimentos: Cod DGP: Quant V.Unit Pos-Aud:",
                "Androstenediona, dosagem 40316076 1 69,18",
                "Dehidrotestosterona (DHT), dosagem 40316220 1 69,18",
                "Cotista: DAP , Credito: D8SAFUSOCSA-FUSEX OCS/C , ND: 339039 Total: 138,36",
                "Obs.: GUIA EM SUBSTITUICAO GUIA 202610573 Valor Devido: 27,67");

        assertEquals(Optional.of(new BigDecimal("138.36")), service.extrair(pdf));
    }

    @Test
    void testExtrair_SemTotal_SomaQuantidadeVezesValorUnitario() throws IOException {
        byte[] pdf = pdfComLinhas(
                "GUIA DE ENCAMINHAMENTO Nr: 202610857 / FUSEX",
                "Capacidade de fixacao de ferro, dosagem 40301427 2 7,87",
                "Ferro serico, dosagem 40301842 1 1.007,87");

        assertEquals(Optional.of(new BigDecimal("1023.61")), service.extrair(pdf));
    }

    @Test
    void testExtrair_SemValores_RetornaVazio() throws IOException {
        byte[] pdf = pdfComLinhas("GUIA DE ENCAMINHAMENTO Nr: 202610857 / FUSEX");

        assertTrue(service.extrair(pdf).isEmpty());
    }

    @Test
    void testExtrair_ArquivoInvalido_RetornaVazio() {
        assertTrue(service.extrair(new byte[]{1, 2, 3}).isEmpty());
        assertTrue(service.extrair(null).isEmpty());
    }
}
