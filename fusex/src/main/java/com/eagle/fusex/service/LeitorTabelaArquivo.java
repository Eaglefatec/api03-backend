package com.eagle.fusex.service;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.dto.response.TabelaArquivoResponse;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class LeitorTabelaArquivo {

    private static final int LIMITE_LINHAS = 500;

    public TabelaArquivoResponse ler(ArquivoLoteDownload arquivo) {
        String nome = arquivo.nome() == null ? "" : arquivo.nome().toLowerCase();
        try {
            if (nome.endsWith(".xlsx")) {
                return new TabelaArquivoResponse(arquivo.nome(), lerXlsx(arquivo.conteudo()));
            }
            if (nome.endsWith(".csv")) {
                return new TabelaArquivoResponse(arquivo.nome(), lerCsv(arquivo.conteudo()));
            }
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler a planilha: " + e.getMessage());
        }
        throw new ArquivoInvalidoException("Somente planilhas (.xlsx ou .csv) podem ser exibidas como tabela");
    }

    private List<List<String>> lerXlsx(byte[] conteudo) throws IOException {
        DataFormatter formatador = new DataFormatter();
        List<List<String>> linhas = new ArrayList<>();
        try (Workbook planilha = WorkbookFactory.create(new ByteArrayInputStream(conteudo))) {
            FormulaEvaluator avaliador = planilha.getCreationHelper().createFormulaEvaluator();
            Sheet aba = planilha.getSheetAt(0);
            int ultima = Math.min(aba.getLastRowNum(), LIMITE_LINHAS - 1);
            for (int i = 0; i <= ultima; i++) {
                Row linha = aba.getRow(i);
                List<String> celulas = new ArrayList<>();
                if (linha != null) {
                    for (int c = 0; c < linha.getLastCellNum(); c++) {
                        celulas.add(linha.getCell(c) == null ? "" : formatador.formatCellValue(linha.getCell(c), avaliador));
                    }
                }
                linhas.add(celulas);
            }
        }
        return linhas;
    }

    private List<List<String>> lerCsv(byte[] conteudo) throws IOException {
        String texto = new String(conteudo, StandardCharsets.UTF_8);
        String primeiraLinha = texto.lines().findFirst().orElse("");
        char separador = primeiraLinha.chars().filter(c -> c == ';').count() >= primeiraLinha.chars().filter(c -> c == ',').count() ? ';' : ',';
        List<List<String>> linhas = new ArrayList<>();
        CSVFormat formato = CSVFormat.DEFAULT.builder().setDelimiter(separador).setIgnoreEmptyLines(false).build();
        try (CSVParser parser = formato.parse(new InputStreamReader(new ByteArrayInputStream(conteudo), StandardCharsets.UTF_8))) {
            for (CSVRecord registro : parser) {
                if (linhas.size() >= LIMITE_LINHAS) {
                    break;
                }
                List<String> celulas = new ArrayList<>();
                registro.forEach(celulas::add);
                linhas.add(celulas);
            }
        }
        return linhas;
    }
}
