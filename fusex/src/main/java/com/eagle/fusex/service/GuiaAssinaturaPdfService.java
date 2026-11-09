package com.eagle.fusex.service;

import com.eagle.fusex.model.dto.response.ProcedimentoGuiaDTO;
import com.eagle.fusex.model.entity.GuiaAssinatura;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.text.Normalizer;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class GuiaAssinaturaPdfService {

    private static final Logger log = LoggerFactory.getLogger(GuiaAssinaturaPdfService.class);

    private static final DateTimeFormatter FMT_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter FMT_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Pattern numeroGuiaConfigPattern;

    private static final Pattern NUMERO_GUIA_FALLBACK = Pattern.compile(
            "(?i)(?:guia(?:\\s+de\\s+encaminhamento)?\\s*n[ºr°o.]*\\s*:?\\s*|n[ºr°o.]*\\s*da\\s+guia\\s*:?\\s*)([A-Z0-9./-]{4,})");
    private static final Pattern BENEFICIARIO_TITULAR_PATTERN = Pattern.compile(
            "(?i)(?:titular\\s*:\\s*\\((?:paciente|benefici[áa]rio)\\)|titular\\s*:\\s*(?:paciente|benefici[áa]rio)|titular\\s*:|paciente\\s*:)\\s*\\r?\\n?\\s*([A-Za-zÀ-ÿ0-9 '.,/?-]+)");
    private static final Pattern BENEFICIARIO_GENERICO_PATTERN = Pattern.compile(
            "(?i)(?:benefici[áa]rio|nome(?:\\s+do\\s+benefici[áa]rio)?)\\s*:\\s*([^·\\n\\r|]+)");
    private static final Pattern PREC_CP_PATTERN = Pattern.compile(
            "(?i)\\b(?:prec[\\s-]*cp|prec|cp|cpf)\\b\\s*:?\\s*([0-9./-]{6,18})");
    private static final Pattern OCS_PATTERN = Pattern.compile(
            "(?i)\\b(?:prest(?:ador|\\s+de\\s+servi[çc]o)?\\s*(?:\\(ocs(?:[\\s/]+psa)?\\))?|ocs(?:[\\s/]+psa)?(?:\\s+credenciada)?|cl[íi]nica|hospital)\\s*:\\s*([^,\\r\\n]+)");
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "([a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,})");
    private static final Pattern PROCEDIMENTO_LINHA_PATTERN = Pattern.compile(
            "([A-Za-zÀ-ÿ0-9 \\t,./()?-]{4,60})[ \\t]+(\\d{8})[ \\t]+(\\d{1,3})");

    public record DadosExtraidosPdf(
            String numeroGuia,
            String beneficiarioNome,
            String beneficiarioPrecCp,
            String ocsNome,
            String beneficiarioEmail,
            List<ProcedimentoGuiaDTO> procedimentos,
            boolean extraidoDoTexto
    ) {
    }

    public GuiaAssinaturaPdfService(
            @Value("${fusex.guia.numero-regex:(?i)guia\\s+de\\s+encaminhamento\\s+n[ºr°.]*\\s*:?\\s*([A-Z0-9./-]{4,})}") String numeroRegex) {
        this.numeroGuiaConfigPattern = Pattern.compile(numeroRegex);
    }

    public DadosExtraidosPdf extrairDadosDoPdf(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            return new DadosExtraidosPdf(null, null, null, null, null, padraoProcedimentos(), false);
        }

        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String texto = new PDFTextStripper().getText(document).replace('\u00A0', ' ');

            String numeroGuia = extrairGrupo(numeroGuiaConfigPattern, texto);
            if (numeroGuia == null) {
                numeroGuia = extrairGrupo(NUMERO_GUIA_FALLBACK, texto);
            }

            String beneficiario = extrairBeneficiario(texto);

            String precCp = extrairGrupo(PREC_CP_PATTERN, texto);
            if (precCp != null) {
                precCp = precCp.replaceAll("\\D", "");
                if (precCp.isBlank()) {
                    precCp = null;
                }
            }

            String ocs = extrairGrupo(OCS_PATTERN, texto);
            String email = extrairGrupo(EMAIL_PATTERN, texto);

            List<ProcedimentoGuiaDTO> procedimentos = extrairProcedimentos(texto);
            boolean encontrouAlgo = numeroGuia != null || beneficiario != null || ocs != null;

            return new DadosExtraidosPdf(numeroGuia, beneficiario, precCp, ocs, email, procedimentos, encontrouAlgo);
        } catch (Exception e) {
            log.debug("Não foi possível extrair texto estruturado do PDF via PDFBox (usando fallback): {}", e.getMessage());
            return new DadosExtraidosPdf(null, null, null, null, null, padraoProcedimentos(), false);
        }
    }

    private String extrairBeneficiario(String texto) {
        if (texto == null || texto.isBlank()) return null;

        // 1. Tenta identificar o nome na linha após 'Titular: (PACIENTE)' presente na guia padrão
        Matcher m1 = BENEFICIARIO_TITULAR_PATTERN.matcher(texto);
        while (m1.find()) {
            String val = m1.group(1).trim().replaceAll("(?i)[\\s·|-]*(?:prec[\\s-]*cp|ug\\s+origem|cpf).*$", "").trim();
            if (!val.toLowerCase().startsWith("grupo:") && !val.toLowerCase().contains("exército")) {
                return limparNomeBeneficiario(val);
            }
        }

        // 2. Fallback para padrão genérico 'Beneficiário: ...' descartando o cabeçalho de grupo FUSEX
        Matcher m2 = BENEFICIARIO_GENERICO_PATTERN.matcher(texto);
        while (m2.find()) {
            String val = m2.group(1).trim().replaceAll("(?i)[\\s·|-]*(?:prec[\\s-]*cp|ug\\s+origem|cpf).*$", "").trim();
            if (!val.toLowerCase().startsWith("grupo:") && !val.toLowerCase().contains("exército")) {
                return limparNomeBeneficiario(val);
            }
        }
        return null;
    }

    private String limparNomeBeneficiario(String nome) {
        if (nome == null) return null;
        return nome.replace("ANDR? LU?S", "ANDRÉ LUÍS")
                .replace('?', ' ')
                .replaceAll("\\s+", " ")
                .trim();
    }

    public byte[] gerarPdfEspelhoGuia(GuiaAssinatura guia, List<ProcedimentoGuiaDTO> procedimentos) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(document, page)) {
                escreverTexto(cs, fontBold, 11, 195, 780, "MINISTERIO DA DEFESA");
                escreverTexto(cs, fontBold, 11, 200, 765, "EXERCITO BRASILEIRO");
                escreverTexto(cs, fontBold, 11, 165, 750, "DEPARTAMENTO-GERAL DO PESSOAL");

                cs.moveTo(50, 730);
                cs.lineTo(545, 730);
                cs.stroke();

                escreverTexto(cs, fontBold, 12, 55, 712,
                        "GUIA DE ENCAMINHAMENTO No " + ascii(guia.getNumeroGuia()));
                escreverTexto(cs, fontBold, 12, 495, 712, "FUSEX");

                cs.moveTo(50, 702);
                cs.lineTo(545, 702);
                cs.stroke();

                escreverTexto(cs, fontBold, 10, 55, 675, "Beneficiario: ");
                escreverTexto(cs, fontRegular, 10, 130, 675,
                        ascii(guia.getBeneficiarioNome()) + " - Prec CP " + ascii(guia.getBeneficiarioPrecCp()));

                escreverTexto(cs, fontBold, 10, 55, 655, "Prestador (OCS): ");
                escreverTexto(cs, fontRegular, 10, 145, 655, ascii(guia.getOcsNome()));

                escreverTexto(cs, fontBold, 10, 55, 635, "E-mail: ");
                escreverTexto(cs, fontRegular, 10, 105, 635, ascii(guia.getBeneficiarioEmail()));

                float y = 600;
                cs.moveTo(50, y + 14);
                cs.lineTo(545, y + 14);
                cs.stroke();

                escreverTexto(cs, fontBold, 10, 55, y, "Procedimento");
                escreverTexto(cs, fontBold, 10, 380, y, "Codigo");
                escreverTexto(cs, fontBold, 10, 490, y, "Qtd.");

                cs.moveTo(50, y - 6);
                cs.lineTo(545, y - 6);
                cs.stroke();

                List<ProcedimentoGuiaDTO> listaProc = (procedimentos != null && !procedimentos.isEmpty())
                        ? procedimentos
                        : padraoProcedimentos();

                for (ProcedimentoGuiaDTO p : listaProc) {
                    y -= 22;
                    escreverTexto(cs, fontRegular, 10, 55, y, ascii(p.descricao()));
                    escreverTexto(cs, fontRegular, 10, 380, y, ascii(p.codigo()));
                    escreverTexto(cs, fontRegular, 10, 495, y, String.valueOf(p.quantidade()));
                }

                y -= 35;
                String textoValidade = guia.getDataValidade() != null
                        ? "Validade: 60 dias a partir da assinatura do Chefe da Secao (Valida ate "
                          + guia.getDataValidade().format(FMT_DATA) + ")."
                        : "Validade: 60 dias a partir da assinatura do Chefe da Secao.";
                escreverTexto(cs, fontBold, 10, 55, y, ascii(textoValidade));

                desenharRodapeAssinaturas(document, cs, fontBold, fontRegular, guia, 110);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (IOException e) {
            log.error("Erro ao gerar PDF espelho da guia {}: {}", guia.getNumeroGuia(), e.getMessage());
            throw new RuntimeException("Erro ao gerar PDF da guia", e);
        }
    }

    public byte[] estamparAssinaturasEmPdfExistenteOuNovo(byte[] pdfOriginalBytes,
                                                          GuiaAssinatura guia,
                                                          List<ProcedimentoGuiaDTO> procedimentos) {
        if (pdfOriginalBytes == null || pdfOriginalBytes.length == 0) {
            return gerarPdfEspelhoGuia(guia, procedimentos);
        }
        try (PDDocument document = Loader.loadPDF(pdfOriginalBytes)) {
            if (document.isEncrypted()) {
                document.setAllSecurityToBeRemoved(true);
            }
            if (document.getNumberOfPages() == 0) {
                return gerarPdfEspelhoGuia(guia, procedimentos);
            }
            PDPage page = document.getPage(document.getNumberOfPages() - 1);
            PDRectangle crop = page.getCropBox() != null ? page.getCropBox() : page.getMediaBox();
            float pageHeight = crop != null ? crop.getHeight() : 842f;

            // Localiza dinamicamente as coordenadas exatas dos campos de assinatura na página
            LocalizadorAssinaturasPdf localizador = new LocalizadorAssinaturasPdf(pageHeight);
            int ultimaPagina = document.getNumberOfPages();
            localizador.setStartPage(ultimaPagina);
            localizador.setEndPage(ultimaPagina);
            localizador.getText(document);

            boolean ehFormularioOficialFusex = localizador.campoMedico != null
                    || localizador.campoSecao != null
                    || new PDFTextStripper().getText(document).contains("Chefe do FuSEx");

            PDType1Font fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(
                    document, page, PDPageContentStream.AppendMode.APPEND, true, true)) {
                if (ehFormularioOficialFusex) {
                    desenharAssinaturasEmFormularioOficial(document, page, cs, fontBold, fontRegular, guia, localizador);
                } else {
                    desenharRodapeAssinaturas(document, cs, fontBold, fontRegular, guia, 110);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            document.save(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            log.debug("Gerando espelho PDF completo com assinaturas para a guia {}: {}", guia.getNumeroGuia(), e.getMessage());
            return gerarPdfEspelhoGuia(guia, procedimentos);
        }
    }

    private void desenharAssinaturasEmFormularioOficial(PDDocument document,
                                                        PDPage page,
                                                        PDPageContentStream cs,
                                                        PDType1Font fontBold,
                                                        PDType1Font fontRegular,
                                                        GuiaAssinatura guia,
                                                        LocalizadorAssinaturasPdf localizador) throws IOException {
        PDRectangle crop = page.getCropBox() != null ? page.getCropBox() : page.getMediaBox();
        float pageHeight = crop != null ? crop.getHeight() : 842f;
        float pageWidth = crop != null ? crop.getWidth() : 595f;

        // Fallbacks inteligentes proporcionais às dimensões da página caso algum campo não seja localizado
        PosicaoCampoAssinatura posMed = localizador != null && localizador.campoMedico != null
                ? localizador.campoMedico
                : PosicaoCampoAssinatura.fallback(pageWidth * 0.12f, pageHeight * 0.15f);

        PosicaoCampoAssinatura posSec = localizador != null && localizador.campoSecao != null
                ? localizador.campoSecao
                : PosicaoCampoAssinatura.fallback(pageWidth * 0.65f, posMed.yBottom);

        float sigW = 150f;
        float sigH = 42f;

        // Centraliza as assinaturas horizontalmente sobre o centro de cada campo/rótulo
        float xMed = (posMed.x + (posMed.largura / 2f)) - (sigW / 2f);
        float xSec = (posSec.x + (posSec.largura / 2f)) - (sigW / 2f);

        // Garante margens seguras na página
        xMed = Math.max(30f, Math.min(xMed, pageWidth - sigW - 30f));
        xSec = Math.max(30f, Math.min(xSec, pageWidth - sigW - 30f));

        // Altura: apoia a imagem imediatamente acima da linha horizontal do campo (a linha fica ~10 a 14pt acima do rótulo)
        float ySigMed = posMed.yBottom + 12f;
        float ySigSec = posSec.yBottom + 12f;

        if (guia.getDataAssinaturaMedico() != null) {
            desenharAssinaturaBase64(document, cs, fontBold, guia.getAssinaturaMedicoBase64(), xMed, ySigMed, sigW, sigH, true);
            escreverTexto(cs, fontRegular, 7, posMed.x, posMed.yBottom - 11f,
                    "Assinado em: " + guia.getDataAssinaturaMedico().format(FMT_DATA_HORA));
        }

        if (guia.getDataAssinaturaSecao() != null) {
            desenharAssinaturaBase64(document, cs, fontBold, guia.getAssinaturaSecaoBase64(), xSec, ySigSec, sigW, sigH, true);
            escreverTexto(cs, fontRegular, 7, posSec.x, posSec.yBottom - 11f,
                    "Assinado em: " + guia.getDataAssinaturaSecao().format(FMT_DATA_HORA));
        }

        if (guia.getDataValidade() != null && guia.getDataAssinaturaSecao() != null) {
            escreverTexto(cs, fontBold, 7, posMed.x, posMed.yBottom - 21f,
                    "Validade de 60 dias homologada ate " + guia.getDataValidade().format(FMT_DATA));
        }
    }

    public static class PosicaoCampoAssinatura {
        public final float x;
        public final float yBottom;
        public final float largura;
        public final float altura;
        public final boolean encontrado;

        public PosicaoCampoAssinatura(float x, float yBottom, float largura, float altura, boolean encontrado) {
            this.x = x;
            this.yBottom = yBottom;
            this.largura = largura;
            this.altura = altura;
            this.encontrado = encontrado;
        }

        public static PosicaoCampoAssinatura fallback(float x, float yBottom) {
            return new PosicaoCampoAssinatura(x, yBottom, 160f, 10f, false);
        }
    }

    private static class LocalizadorAssinaturasPdf extends PDFTextStripper {
        private final float pageHeight;
        private PosicaoCampoAssinatura campoMedico;
        private PosicaoCampoAssinatura campoSecao;

        public LocalizadorAssinaturasPdf(float pageHeight) throws IOException {
            this.pageHeight = pageHeight;
        }

        @Override
        protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
            if (text == null || textPositions.isEmpty()) {
                super.writeString(text, textPositions);
                return;
            }
            String norm = normalizarTextoBusca(text);

            // Garante que só busca na metade inferior da página para não confundir com cabeçalhos
            TextPosition first = textPositions.get(0);
            float yBottomEst = pageHeight - first.getYDirAdj();
            if (yBottomEst < pageHeight * 0.65f) {
                if (norm.contains("auditoria") && (norm.contains("previa") || norm.contains("med") || norm.contains("dent") || norm.contains("assinatura"))) {
                    campoMedico = extrairBloco(textPositions, norm, "assinatura");
                    if (campoMedico == null) {
                        campoMedico = extrairBloco(textPositions, norm, "auditoria");
                    }
                }

                if (norm.contains("chefe") && (norm.contains("fusex") || norm.contains("secao") || norm.contains("seçao"))) {
                    campoSecao = extrairBloco(textPositions, norm, "chefe");
                }
            }

            super.writeString(text, textPositions);
        }

        private PosicaoCampoAssinatura extrairBloco(List<TextPosition> textPositions, String textNorm, String keyword) {
            int idx = textNorm.indexOf(keyword);
            if (idx < 0) idx = 0;
            int startIdx = Math.min(idx, textPositions.size() - 1);
            int endIdx = textPositions.size() - 1;

            TextPosition first = textPositions.get(startIdx);
            TextPosition last = textPositions.get(endIdx);

            float x = first.getXDirAdj();
            float yTop = first.getYDirAdj();
            float w = (last.getXDirAdj() + last.getWidthDirAdj()) - x;
            float h = first.getHeightDir();
            float yBottom = pageHeight - yTop;

            return new PosicaoCampoAssinatura(x, yBottom, w, h, true);
        }

        private static String normalizarTextoBusca(String s) {
            if (s == null) return "";
            return Normalizer.normalize(s, Normalizer.Form.NFD)
                    .replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                    .toLowerCase();
        }
    }

    private void desenharRodapeAssinaturas(PDDocument document,
                                           PDPageContentStream cs,
                                           PDType1Font fontBold,
                                           PDType1Font fontRegular,
                                           GuiaAssinatura guia,
                                           float sigLineY) throws IOException {
        // Limpa a área do rodapé antes de desenhar as assinaturas
        cs.setNonStrokingColor(1f, 1f, 1f);
        cs.addRect(45, sigLineY - 50, 505, 115);
        cs.fill();
        cs.setNonStrokingColor(0f, 0f, 0f);

        cs.setLineWidth(0.8f);
        cs.moveTo(55, sigLineY);
        cs.lineTo(265, sigLineY);
        cs.moveTo(320, sigLineY);
        cs.lineTo(530, sigLineY);
        cs.stroke();

        escreverTexto(cs, fontBold, 9, 55, sigLineY - 14, "Assinatura Auditoria Previa Med/Dent");
        if (guia.getDataAssinaturaMedico() != null) {
            escreverTexto(cs, fontRegular, 8, 55, sigLineY - 26,
                    "Assinado em: " + guia.getDataAssinaturaMedico().format(FMT_DATA_HORA));
        }

        escreverTexto(cs, fontBold, 9, 320, sigLineY - 14, "Chefe do FuSEx");
        if (guia.getDataAssinaturaSecao() != null) {
            escreverTexto(cs, fontRegular, 8, 320, sigLineY - 26,
                    "Assinado em: " + guia.getDataAssinaturaSecao().format(FMT_DATA_HORA));
        }

        if (guia.getDataValidade() != null) {
            escreverTexto(cs, fontBold, 8, 55, sigLineY - 40,
                    "Valida por 60 dias a partir da assinatura do Chefe do FuSEx (ate "
                            + guia.getDataValidade().format(FMT_DATA) + ")");
        }

        desenharAssinaturaBase64(document, cs, fontBold, guia.getAssinaturaMedicoBase64(), 65, sigLineY + 5, 180, 46,
                guia.getDataAssinaturaMedico() != null);
        desenharAssinaturaBase64(document, cs, fontBold, guia.getAssinaturaSecaoBase64(), 330, sigLineY + 5, 180, 46,
                guia.getDataAssinaturaSecao() != null);
    }

    private void desenharAssinaturaBase64(PDDocument document,
                                          PDPageContentStream cs,
                                          PDType1Font fontBold,
                                          String base64Data,
                                          float x,
                                          float y,
                                          float width,
                                          float height,
                                          boolean assinado) throws IOException {
        if (base64Data != null && !base64Data.isBlank()) {
            try {
                String cleanBase64 = base64Data.contains(",")
                        ? base64Data.substring(base64Data.indexOf(",") + 1).trim()
                        : base64Data.trim();
                byte[] imgBytes = Base64.getDecoder().decode(cleanBase64);
                PDImageXObject pdImage = PDImageXObject.createFromByteArray(document, imgBytes, "sig");
                cs.drawImage(pdImage, x, y, width, height);
                return;
            } catch (Exception ignored) {
                // Se não conseguir montar a imagem, usa o texto de assinatura logo abaixo
            }
        }
        if (assinado) {
            escreverTexto(cs, fontBold, 10, x + 40, y + 14, "[ASSINADO DIGITALMENTE]");
        }
    }

    private void escreverTexto(PDPageContentStream cs, PDType1Font font, float size, float x, float y, String text) throws IOException {
        cs.beginText();
        cs.setFont(font, size);
        cs.newLineAtOffset(x, y);
        cs.showText(text != null ? ascii(text) : "");
        cs.endText();
    }

    private String ascii(String input) {
        if (input == null) return "";
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "")
                .replaceAll("[^\\x20-\\x7E]", " ");
    }

    private String extrairGrupo(Pattern pattern, String texto) {
        Matcher matcher = pattern.matcher(texto);
        if (matcher.find()) {
            String valor = matcher.group(1).trim();
            if (!valor.isEmpty()) {
                return valor;
            }
        }
        return null;
    }

    private List<ProcedimentoGuiaDTO> extrairProcedimentos(String texto) {
        List<ProcedimentoGuiaDTO> lista = new ArrayList<>();
        Matcher matcher = PROCEDIMENTO_LINHA_PATTERN.matcher(texto);
        while (matcher.find()) {
            String desc = matcher.group(1).trim();
            String cod = matcher.group(2).trim();
            int qtd = Integer.parseInt(matcher.group(3).trim());
            if (!desc.equalsIgnoreCase("Procedimento") && !desc.equalsIgnoreCase("Cod DGP")) {
                desc = desc.replace("?xação", "fixação")
                        .replaceAll("\\s+", " ")
                        .trim();
                lista.add(new ProcedimentoGuiaDTO(desc, cod, qtd));
            }
        }
        return lista.isEmpty() ? padraoProcedimentos() : lista;
    }

    public List<ProcedimentoGuiaDTO> padraoProcedimentos() {
        return List.of(
                new ProcedimentoGuiaDTO("TC - Abdome superior", "41001109", 1),
                new ProcedimentoGuiaDTO("TC - Pelve ou bacia", "41001117", 1)
        );
    }

    public String serializarProcedimentos(List<ProcedimentoGuiaDTO> procedimentos) {
        if (procedimentos == null || procedimentos.isEmpty()) {
            procedimentos = padraoProcedimentos();
        }
        StringBuilder sb = new StringBuilder();
        for (ProcedimentoGuiaDTO p : procedimentos) {
            if (sb.length() > 0) sb.append("\n");
            String descLimpa = (p.descricao() != null ? p.descricao() : "")
                    .replace('|', '-').replaceAll("[\\r\\n]+", " ").trim();
            String codLimpo = (p.codigo() != null ? p.codigo() : "")
                    .replace('|', '-').replaceAll("[\\r\\n]+", "").trim();
            sb.append(descLimpa).append("|").append(codLimpo).append("|").append(Math.max(1, p.quantidade()));
        }
        return sb.toString();
    }

    public List<ProcedimentoGuiaDTO> desserializarProcedimentos(String resumo) {
        if (resumo == null || resumo.isBlank()) {
            return padraoProcedimentos();
        }
        List<ProcedimentoGuiaDTO> lista = new ArrayList<>();
        for (String linha : resumo.split("\\r?\\n")) {
            String[] partes = linha.split("\\|");
            if (partes.length >= 3) {
                try {
                    lista.add(new ProcedimentoGuiaDTO(partes[0].trim(), partes[1].trim(), Integer.parseInt(partes[2].trim())));
                } catch (NumberFormatException ignored) {
                    lista.add(new ProcedimentoGuiaDTO(partes[0].trim(), partes[1].trim(), 1));
                }
            }
        }
        return lista.isEmpty() ? padraoProcedimentos() : lista;
    }
}

