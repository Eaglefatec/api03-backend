package com.eagle.fusex.service;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.dto.response.TabelaArquivoResponse;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.*;

class LeitorTabelaArquivoTest {

    private final LeitorTabelaArquivo leitor = new LeitorTabelaArquivo();

    @Test
    void testLerXlsx_RetornaLinhasEColunas() throws Exception {
        byte[] conteudo;
        try (XSSFWorkbook planilha = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            var aba = planilha.createSheet();
            aba.createRow(0).createCell(0).setCellValue("Nº da Guia");
            var linha = aba.createRow(1);
            linha.createCell(0).setCellValue("202610836");
            linha.createCell(1).setCellValue(150.5);
            planilha.write(saida);
            conteudo = saida.toByteArray();
        }

        TabelaArquivoResponse tabela = leitor.ler(new ArquivoLoteDownload(conteudo, "espelho.xlsx", null));

        assertEquals(2, tabela.linhas().size());
        assertEquals("Nº da Guia", tabela.linhas().get(0).get(0));
        assertEquals("202610836", tabela.linhas().get(1).get(0));
    }

    @Test
    void testLerXlsx_CalculaFormulas() throws Exception {
        byte[] conteudo;
        try (XSSFWorkbook planilha = new XSSFWorkbook(); ByteArrayOutputStream saida = new ByteArrayOutputStream()) {
            var aba = planilha.createSheet();
            aba.createRow(0).createCell(0).setCellValue(10);
            aba.createRow(1).createCell(0).setCellValue(5);
            aba.createRow(2).createCell(0).setCellFormula("SUM(A1:A2)");
            planilha.write(saida);
            conteudo = saida.toByteArray();
        }

        TabelaArquivoResponse tabela = leitor.ler(new ArquivoLoteDownload(conteudo, "espelho.xlsx", null));

        assertEquals("15", tabela.linhas().get(2).get(0));
    }

    @Test
    void testLerCsv_DetectaPontoEVirgula() {
        byte[] conteudo = "guia;valor\n123;10,50\n".getBytes(StandardCharsets.UTF_8);

        TabelaArquivoResponse tabela = leitor.ler(new ArquivoLoteDownload(conteudo, "espelho.csv", null));

        assertEquals(2, tabela.linhas().size());
        assertEquals("10,50", tabela.linhas().get(1).get(1));
    }

    @Test
    void testLer_PdfNaoEhPlanilha_LancaExcecao() {
        assertThrows(ArquivoInvalidoException.class,
                () -> leitor.ler(new ArquivoLoteDownload(new byte[]{1}, "fatura.pdf", "application/pdf")));
    }
}
