package com.eagle.fusex.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GuiaValorContratadoPdfService {

    private static final String VALOR = "(\\d{1,3}(?:\\.\\d{3})+,\\d{2}|\\d+,\\d{2})";
    private static final Pattern TOTAL = Pattern.compile("(?i)(?<!\\p{L})total\\s*:\\s*(?:R\\$\\s*)?" + VALOR);
    private static final Pattern ITEM = Pattern.compile("\\b(\\d{8})[ \\t]+(\\d{1,3})[ \\t]+" + VALOR);

    public Optional<BigDecimal> extrair(byte[] pdf) {
        if (pdf == null || pdf.length == 0) {
            return Optional.empty();
        }
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            String texto = new PDFTextStripper().getText(documento).replace(' ', ' ');
            return valorTotal(texto).or(() -> somaDosItens(texto));
        } catch (IOException | RuntimeException e) {
            return Optional.empty();
        }
    }

    private Optional<BigDecimal> valorTotal(String texto) {
        Matcher matcher = TOTAL.matcher(texto);
        if (matcher.find()) {
            return positivo(converter(matcher.group(1)));
        }
        return Optional.empty();
    }

    private Optional<BigDecimal> somaDosItens(String texto) {
        Matcher matcher = ITEM.matcher(texto);
        BigDecimal soma = BigDecimal.ZERO;
        while (matcher.find()) {
            BigDecimal quantidade = BigDecimal.valueOf(Integer.parseInt(matcher.group(2)));
            soma = soma.add(converter(matcher.group(3)).multiply(quantidade));
        }
        return positivo(soma);
    }

    private static Optional<BigDecimal> positivo(BigDecimal valor) {
        return valor.signum() > 0 ? Optional.of(valor) : Optional.empty();
    }

    static BigDecimal converter(String valor) {
        return new BigDecimal(valor.replace(".", "").replace(',', '.'));
    }
}
