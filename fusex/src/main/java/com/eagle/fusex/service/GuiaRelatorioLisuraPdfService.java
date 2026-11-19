package com.eagle.fusex.service;

import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.entity.Glosa;
import com.eagle.fusex.model.entity.Guia;
import com.eagle.fusex.model.entity.GuiaItem;
import com.eagle.fusex.repository.GlosaRepository;
import com.eagle.fusex.repository.GuiaItemRepository;
import com.eagle.fusex.repository.GuiaRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class GuiaRelatorioLisuraPdfService {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final float MARGEM = 50;

    private final GuiaRepository guiaRepository;
    private final GlosaRepository glosaRepository;
    private final GuiaItemRepository guiaItemRepository;

    public GuiaRelatorioLisuraPdfService(GuiaRepository guiaRepository,
                                         GlosaRepository glosaRepository,
                                         GuiaItemRepository guiaItemRepository) {
        this.guiaRepository = guiaRepository;
        this.glosaRepository = glosaRepository;
        this.guiaItemRepository = guiaItemRepository;
    }

    @Transactional(readOnly = true)
    public ArquivoLoteDownload gerarGuiaComRelatorio(Long guiaId) {
        Guia guia = guiaRepository.findById(guiaId)
                .orElseThrow(() -> new ResourceNotFoundException("Guia", guiaId));
        byte[] original = lerArquivoGuia(guia);
        List<GuiaItem> itens = guiaItemRepository.findByGuiaId(guiaId);
        List<Glosa> glosas = new ArrayList<>(glosaRepository.findByGuiaId(guiaId));
        glosas.sort(Comparator.comparing(Glosa::getDataRegistro, Comparator.nullsLast(Comparator.naturalOrder())));

        try (PDDocument documento = Loader.loadPDF(original)) {
            PDRectangle tamanho = documento.getNumberOfPages() > 0
                    ? documento.getPage(documento.getNumberOfPages() - 1).getMediaBox()
                    : PDRectangle.A4;
            new Relatorio(documento, tamanho).escrever(guia, itens, glosas);
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            documento.save(saida);
            return new ArquivoLoteDownload(saida.toByteArray(),
                    "guia_" + guia.getNumeroGuia() + "_relatorio_lisura.pdf", "application/pdf");
        } catch (IOException e) {
            throw new ResourceNotFoundException("Não foi possível montar o PDF da guia com o relatório");
        }
    }

    private byte[] lerArquivoGuia(Guia guia) {
        if (guia.getCaminhoArquivo() == null) {
            throw new ResourceNotFoundException("Arquivo da guia não encontrado");
        }
        Path caminho = Path.of(guia.getCaminhoArquivo());
        if (!Files.exists(caminho)) {
            throw new ResourceNotFoundException("Arquivo da guia não encontrado");
        }
        try {
            return Files.readAllBytes(caminho);
        } catch (IOException e) {
            throw new ResourceNotFoundException("Não foi possível ler o arquivo da guia");
        }
    }

    static BigDecimal valorNoEspelho(Guia guia, List<GuiaItem> itens) {
        if (itens.isEmpty()) {
            return guia.getValorFaturado() != null ? guia.getValorFaturado() : BigDecimal.ZERO;
        }
        return itens.stream()
                .map(GuiaRelatorioLisuraPdfService::subtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static BigDecimal subtotal(GuiaItem item) {
        BigDecimal unitario = item.getValorUnitario() != null ? item.getValorUnitario() : BigDecimal.ZERO;
        int quantidade = item.getQuantidade() != null ? item.getQuantidade() : 0;
        return unitario.multiply(BigDecimal.valueOf(quantidade));
    }

    static String situacao(BigDecimal diferenca, List<Glosa> glosas) {
        if (!glosas.isEmpty()) {
            return "GLOSADA / COM OBSERVAÇÃO";
        }
        return diferenca.signum() != 0 ? "DIVERGENTE - SEM GLOSA REGISTRADA" : "CONFERIDA - SEM DIVERGÊNCIA";
    }

    private static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"))
                .format(valor != null ? valor : BigDecimal.ZERO)
                .replace(' ', ' ');
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor.trim();
    }

    private static final class Relatorio {

        private final PDDocument documento;
        private final PDRectangle tamanho;
        private final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font negrito = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private PDPageContentStream conteudo;
        private float y;

        Relatorio(PDDocument documento, PDRectangle tamanho) {
            this.documento = documento;
            this.tamanho = tamanho;
        }

        void escrever(Guia guia, List<GuiaItem> itens, List<Glosa> glosas) throws IOException {
            novaPagina();
            try {
                cabecalho(guia);
                BigDecimal contratado = guia.getValorContratado() != null ? guia.getValorContratado() : BigDecimal.ZERO;
                BigDecimal espelho = valorNoEspelho(guia, itens);
                BigDecimal diferenca = contratado.subtract(espelho);

                secao("DADOS DA GUIA");
                campo("Beneficiário", texto(guia.getBeneficiarioNome()) + "   Prec CP: " + texto(guia.getBeneficiarioPrecCp()));
                campo("OCS", guia.getOcs() != null ? texto(guia.getOcs().getOcsNome()) : "-");
                campo("Data do atendimento", guia.getDataAtendimento() != null ? DATA.format(guia.getDataAtendimento()) : "-");
                campo("Lote", guia.getLote() != null && guia.getLote().getNumeroLote() != null
                        ? String.valueOf(guia.getLote().getNumeroLote()) : "-");

                secao("CONFERÊNCIA DE VALORES");
                campo("Valor contratado", moeda(contratado));
                campo("Valor no espelho", moeda(espelho));
                campo("Diferença", moeda(diferenca));
                campo("Situação", situacao(diferenca, glosas));

                if (!itens.isEmpty()) {
                    secao("ITENS DO ESPELHO");
                    for (GuiaItem item : itens) {
                        linhaQuebrada(normal, 9, "- " + texto(item.getCodigoTuss()) + " · " + texto(item.getDescricaoItem())
                                + " (" + texto(item.getTipo()) + ") · Qtd " + (item.getQuantidade() != null ? item.getQuantidade() : 0)
                                + " x " + moeda(item.getValorUnitario()) + " = " + moeda(subtotal(item)), 10);
                    }
                }

                secao("GLOSAS E OBSERVAÇÕES DA AUDITORIA");
                if (glosas.isEmpty()) {
                    linhaQuebrada(normal, 10, "Nenhuma glosa ou observação registrada para esta guia.", 0);
                }
                int n = 1;
                for (Glosa glosa : glosas) {
                    garantirEspaco(70);
                    linhaQuebrada(negrito, 10, n++ + ". Registrada em "
                            + (glosa.getDataRegistro() != null ? DATA_HORA.format(glosa.getDataRegistro()) : "-")
                            + (glosa.getAuditor() != null ? " por " + glosa.getAuditor() : ""), 0);
                    campo("Código TUSS", texto(glosa.getCodigoTuss()));
                    campo("Valor revisado", moeda(glosa.getValorGlosa()));
                    campo("Justificativa", texto(glosa.getJustificativaMilitar()));
                    campo("Conclusão", texto(glosa.getConclusao()));
                    y -= 6;
                }

                y -= 10;
                linhaQuebrada(normal, 8, "Relatório gerado em " + DATA_HORA.format(LocalDateTime.now())
                        + ". Impresso no verso da guia " + guia.getNumeroGuia() + ".", 0);
            } finally {
                conteudo.close();
            }
        }

        private void cabecalho(Guia guia) throws IOException {
            linhaQuebrada(negrito, 9, "MINISTÉRIO DA DEFESA - EXÉRCITO BRASILEIRO - FUSEX", 0);
            y -= 4;
            linhaQuebrada(negrito, 14, "RELATÓRIO DE AUDITORIA DA GUIA (LISURA)", 0);
            linhaQuebrada(negrito, 12, "Guia Nº " + texto(guia.getNumeroGuia()), 0);
            y -= 2;
            conteudo.setStrokingColor(Color.DARK_GRAY);
            conteudo.moveTo(MARGEM, y);
            conteudo.lineTo(tamanho.getWidth() - MARGEM, y);
            conteudo.stroke();
            y -= 8;
        }

        private void secao(String titulo) throws IOException {
            garantirEspaco(40);
            y -= 8;
            linhaQuebrada(negrito, 11, titulo, 0);
            y -= 2;
        }

        private void campo(String rotulo, String valor) throws IOException {
            linhaQuebrada(normal, 10, rotulo + ": " + valor, 0);
        }

        private void linhaQuebrada(PDType1Font fonte, float corpo, String valor, float recuo) throws IOException {
            float largura = tamanho.getWidth() - 2 * MARGEM - recuo;
            for (String paragrafo : seguro(fonte, valor).split("\n")) {
                for (String linha : quebrar(fonte, corpo, paragrafo, largura)) {
                    garantirEspaco(corpo + 4);
                    conteudo.beginText();
                    conteudo.setFont(fonte, corpo);
                    conteudo.newLineAtOffset(MARGEM + recuo, y - corpo);
                    conteudo.showText(linha);
                    conteudo.endText();
                    y -= corpo + 4;
                }
            }
        }

        private List<String> quebrar(PDType1Font fonte, float corpo, String paragrafo, float largura) throws IOException {
            List<String> linhas = new ArrayList<>();
            StringBuilder atual = new StringBuilder();
            for (String palavra : paragrafo.split(" ")) {
                String tentativa = atual.isEmpty() ? palavra : atual + " " + palavra;
                if (fonte.getStringWidth(tentativa) / 1000 * corpo <= largura || atual.isEmpty()) {
                    atual.setLength(0);
                    atual.append(tentativa);
                } else {
                    linhas.add(atual.toString());
                    atual.setLength(0);
                    atual.append(palavra);
                }
            }
            linhas.add(atual.toString());
            return linhas;
        }

        private String seguro(PDType1Font fonte, String valor) {
            StringBuilder sb = new StringBuilder();
            for (char c : valor.replace("\r", "").replace('\t', ' ').toCharArray()) {
                if (c == '\n') {
                    sb.append(c);
                    continue;
                }
                try {
                    fonte.encode(String.valueOf(c));
                    sb.append(c);
                } catch (IOException | IllegalArgumentException e) {
                    sb.append('?');
                }
            }
            return sb.toString();
        }

        private void garantirEspaco(float altura) throws IOException {
            if (y - altura < MARGEM) {
                conteudo.close();
                novaPagina();
            }
        }

        private void novaPagina() throws IOException {
            PDPage pagina = new PDPage(new PDRectangle(tamanho.getWidth(), tamanho.getHeight()));
            documento.addPage(pagina);
            conteudo = new PDPageContentStream(documento, pagina);
            y = tamanho.getHeight() - MARGEM;
        }
    }
}
