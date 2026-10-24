package com.eagle.fusex.service.importacao.util;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.time.LocalDate;
import java.util.regex.Pattern;

public final class PlanilhaUtils {

    private static final Pattern MARCAS_DIACRITICAS = Pattern.compile("\\p{M}");
    private static final Pattern ESPACOS = Pattern.compile("\\s+");

    private PlanilhaUtils() {
    }

    public static String normalizar(String texto) {
        if (texto == null) {
            return "";
        }
        String semAcento = MARCAS_DIACRITICAS.matcher(
                Normalizer.normalize(texto.trim().toLowerCase(), Normalizer.Form.NFKD)
        ).replaceAll("");
        return ESPACOS.matcher(semAcento).replaceAll(" ").trim();
    }

    public static boolean vazia(Row row, int coluna) {
        if (row == null) {
            return true;
        }
        Cell cell = row.getCell(coluna);
        if (cell == null || cell.getCellType() == CellType.BLANK) {
            return true;
        }
        return cell.getCellType() == CellType.STRING && cell.getStringCellValue().isBlank();
    }

    public static String textoDaCelula(Row row, int coluna) {
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(coluna);
        if (cell == null) {
            return null;
        }
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getLocalDateTimeCellValue().toLocalDate().toString();
                }
                double valor = cell.getNumericCellValue();
                yield valor == Math.floor(valor) ? String.valueOf((long) valor) : String.valueOf(valor);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    public static LocalDate dataDaCelula(Row row, int coluna) {
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(coluna);
        if (cell == null || cell.getCellType() != CellType.NUMERIC || !DateUtil.isCellDateFormatted(cell)) {
            return null;
        }
        return cell.getLocalDateTimeCellValue().toLocalDate();
    }

    public static BigDecimal valorDaCelula(Row row, int coluna) {
        if (row == null) {
            return null;
        }
        Cell cell = row.getCell(coluna);
        if (cell == null || cell.getCellType() != CellType.NUMERIC) {
            return null;
        }
        return BigDecimal.valueOf(cell.getNumericCellValue());
    }
}
