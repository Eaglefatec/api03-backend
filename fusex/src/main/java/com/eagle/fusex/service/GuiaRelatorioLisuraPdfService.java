package com.eagle.fusex.service;

import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.entity.Glosa;
import com.eagle.fusex.model.entity.Guia;
import com.eagle.fusex.model.entity.GuiaItem;
import com.eagle.fusex.model.entity.Lote;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.repository.GlosaRepository;
import com.eagle.fusex.repository.GuiaItemRepository;
import com.eagle.fusex.repository.GuiaRepository;
import com.eagle.fusex.repository.PacienteRepository;
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

    private static final DateTimeFormatter DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final DateTimeFormatter GERADO_EM = DateTimeFormatter.ofPattern("dd/MM/yyyy, HH:mm");

    private final GuiaRepository guiaRepository;
    private final GlosaRepository glosaRepository;
    private final GuiaItemRepository guiaItemRepository;
    private final PacienteRepository pacienteRepository;

    public GuiaRelatorioLisuraPdfService(GuiaRepository guiaRepository,
                                         GlosaRepository glosaRepository,
                                         GuiaItemRepository guiaItemRepository,
                                         PacienteRepository pacienteRepository) {
        this.guiaRepository = guiaRepository;
        this.glosaRepository = glosaRepository;
        this.guiaItemRepository = guiaItemRepository;
        this.pacienteRepository = pacienteRepository;
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
            new Relatorio(documento, montarDados(guia, itens, glosas)).escrever();
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

    private Dados montarDados(Guia guia, List<GuiaItem> itens, List<Glosa> glosas) {
        BigDecimal contratado = guia.getValorContratado() != null ? guia.getValorContratado() : BigDecimal.ZERO;
        BigDecimal espelho = valorNoEspelho(guia, itens);
        Ocs ocs = guia.getOcs();
        Lote lote = guia.getLote();
        return new Dados(
                texto(guia.getNumeroGuia()),
                nomeBeneficiario(guia),
                texto(guia.getBeneficiarioPrecCp()),
                ocs != null ? texto(ocs.getOcsNome()) : "-",
                ocs != null ? cnpj(ocs.getOcsInscricaoFederal()) : "-",
                competenciaLote(lote),
                lote != null ? texto(lote.getNumeroSisfat()) : "-",
                lote != null && lote.getFechamento() != null ? lote.getFechamento().getId() : null,
                contratado, espelho, itens, glosas);
    }

    private String nomeBeneficiario(Guia guia) {
        if (guia.getBeneficiarioNome() != null && !guia.getBeneficiarioNome().isBlank()) {
            return guia.getBeneficiarioNome().trim();
        }
        if (pacienteRepository != null && guia.getBeneficiarioPrecCp() != null) {
            return pacienteRepository.findByPrec(guia.getBeneficiarioPrecCp())
                    .map(Paciente::getNome)
                    .orElse("-");
        }
        return "-";
    }

    private static String competenciaLote(Lote lote) {
        if (lote == null) {
            return "-";
        }
        String competencia = lote.getCompetenciaMes() != null && lote.getCompetenciaAno() != null
                ? String.format("%02d/%d", lote.getCompetenciaMes(), lote.getCompetenciaAno())
                : "-";
        String numero = lote.getNumeroLote() != null ? String.format("Lote %02d", lote.getNumeroLote()) : "Lote -";
        return competencia + " · " + numero;
    }

    private static String cnpj(String valor) {
        if (valor == null || valor.isBlank()) {
            return "-";
        }
        String digitos = valor.replaceAll("\\D", "");
        if (digitos.length() != 14) {
            return valor.trim();
        }
        return digitos.replaceFirst("(\\d{2})(\\d{3})(\\d{3})(\\d{4})(\\d{2})", "$1.$2.$3/$4-$5");
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

    private static String moeda(BigDecimal valor) {
        return NumberFormat.getCurrencyInstance(Locale.of("pt", "BR"))
                .format(valor != null ? valor : BigDecimal.ZERO)
                .replace(' ', ' ');
    }

    private static String texto(String valor) {
        return valor == null || valor.isBlank() ? "-" : valor.trim();
    }

    private record Dados(String numeroGuia, String beneficiario, String prec, String ocs, String cnpj,
                         String competenciaLote, String sisfat, Long fechamentoId,
                         BigDecimal contratado, BigDecimal espelho, List<GuiaItem> itens, List<Glosa> glosas) {

        BigDecimal diferenca() {
            return espelho.subtract(contratado);
        }

        boolean semPrecoContratado() {
            return contratado.signum() <= 0;
        }

        Glosa ultimaGlosa() {
            return glosas.isEmpty() ? null : glosas.get(glosas.size() - 1);
        }
    }

    private static final class Relatorio {

        private static final float MARGEM = 46;
        private static final float RODAPE = 130;
        private static final Color AZUL = new Color(0x1E, 0x3A, 0x6E);
        private static final Color TEXTO = new Color(0x1F, 0x29, 0x37);
        private static final Color CINZA = new Color(0x6B, 0x72, 0x80);
        private static final Color LINHA = new Color(0xE2, 0xE8, 0xF0);
        private static final Color FUNDO = new Color(0xF1, 0xF5, 0xF9);
        private static final Color VERMELHO = new Color(0xB9, 0x1C, 0x1C);

        private final PDDocument documento;
        private final Dados dados;
        private final PDType1Font normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
        private final PDType1Font negrito = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
        private final PDRectangle tamanho = PDRectangle.A4;
        private final float largura = PDRectangle.A4.getWidth() - 2 * MARGEM;
        private PDPageContentStream conteudo;
        private float y;

        Relatorio(PDDocument documento, Dados dados) {
            this.documento = documento;
            this.dados = dados;
        }

        void escrever() throws IOException {
            novaPagina(false);
            try {
                cabecalho();
                identificacao();
                cruzamento();
                itens();
                parecer();
                rodape();
            } finally {
                conteudo.close();
            }
        }

        private void cabecalho() throws IOException {
            float centro = tamanho.getWidth() / 2;
            centralizado(negrito, 7.5f, TEXTO, "MINISTÉRIO DA DEFESA · EXÉRCITO BRASILEIRO · FUSEX", centro, y);
            y -= 22;
            centralizado(negrito, 14, AZUL, "Relatório de Lisura", centro, y);
            y -= 14;
            centralizado(normal, 9, CINZA, "Verso da guia nº " + dados.numeroGuia(), centro, y);
            y -= 30;
        }

        private void identificacao() throws IOException {
            secao("Identificação");
            String[][] linhas = {
                    {"Beneficiário", dados.beneficiario(), "PREC/CP", dados.prec()},
                    {"OCS", dados.ocs(), "CNPJ", dados.cnpj()},
                    {"Competência · Lote", dados.competenciaLote(), "Fatura (SisFat)", dados.sisfat()}
            };
            float coluna = (largura - 16) / 2;
            for (String[] linha : linhas) {
                parChaveValor(MARGEM, coluna, linha[0], linha[1]);
                parChaveValor(MARGEM + coluna + 16, coluna, linha[2], linha[3]);
                retaH(MARGEM, MARGEM + coluna, y - 8, LINHA, 0.6f);
                retaH(MARGEM + coluna + 16, MARGEM + largura, y - 8, LINHA, 0.6f);
                y -= 22;
            }
            y -= 10;
        }

        private void parChaveValor(float x, float coluna, String chave, String valor) throws IOException {
            escrever(normal, 8, CINZA, chave, x, y);
            float espaco = coluna - normal.getStringWidth(seguro(normal, chave)) / 1000 * 8 - 10;
            aDireita(negrito, 8.5f, TEXTO, ajustar(negrito, 8.5f, valor, espaco), x + coluna, y);
        }

        private void cruzamento() throws IOException {
            secao("Cruzamento de valores");
            boolean semPreco = dados.semPrecoContratado();
            BigDecimal diferenca = dados.diferenca();
            String textoDiferenca = semPreco ? "Não calculada"
                    : diferenca.signum() == 0 ? "Sem divergência"
                    : (diferenca.signum() > 0 ? "+" : "-") + moeda(diferenca.abs());
            Glosa ultima = dados.ultimaGlosa();
            String revisado = ultima != null ? moeda(ultima.getValorGlosa())
                    : !semPreco && diferenca.signum() == 0 ? moeda(dados.espelho()) : "Sem revisão";
            String[][] caixas = {
                    {"Contratado (guia)", semPreco ? "Não cadastrado" : moeda(dados.contratado())},
                    {"No espelho", moeda(dados.espelho())},
                    {"Diferença", textoDiferenca},
                    {"Valor revisado", revisado}
            };
            float caixa = largura / 4;
            float altura = 38;
            conteudo.setStrokingColor(LINHA);
            conteudo.setLineWidth(0.8f);
            conteudo.addRect(MARGEM, y - altura, largura, altura);
            conteudo.stroke();
            for (int i = 0; i < caixas.length; i++) {
                float x = MARGEM + i * caixa;
                if (i > 0) {
                    retaV(x, y, y - altura, LINHA, 0.8f);
                }
                escrever(normal, 7.5f, CINZA, caixas[i][0], x + 10, y - 13);
                Color cor = (i == 0 && semPreco) || (i == 2 && !semPreco && diferenca.signum() != 0) ? VERMELHO : TEXTO;
                escrever(negrito, 11, cor, ajustar(negrito, 11, caixas[i][1], caixa - 20), x + 10, y - 29);
            }
            y -= altura + 22;
        }

        private void itens() throws IOException {
            secao("Itens do espelho");
            float[] x = {MARGEM + 8, MARGEM + largura * 0.17f, MARGEM + largura * 0.30f};
            float qtd = MARGEM + largura * 0.68f;
            float unitario = MARGEM + largura * 0.84f;
            float subtotal = MARGEM + largura - 8;
            float larguraDescricao = qtd - x[2] - 34;

            cabecalhoTabela(x, qtd, unitario, subtotal);
            if (dados.itens().isEmpty()) {
                y -= 4;
                escrever(normal, 8.5f, CINZA, "Nenhum item detalhado no espelho. Valor consolidado: "
                        + moeda(dados.espelho()) + ".", x[0], y - 8);
                y -= 22;
            }
            for (GuiaItem item : dados.itens()) {
                List<String> descricao = quebrar(normal, 8.5f, texto(item.getDescricaoItem()), larguraDescricao);
                float altura = 8 + descricao.size() * 11;
                if (y - altura < RODAPE) {
                    novaPagina(true);
                    cabecalhoTabela(x, qtd, unitario, subtotal);
                }
                float base = y - 12;
                escrever(normal, 8.5f, TEXTO, ajustar(normal, 8.5f, texto(item.getTipo()), x[1] - x[0] - 6), x[0], base);
                escrever(normal, 8.5f, TEXTO, texto(item.getCodigoTuss()), x[1], base);
                for (int i = 0; i < descricao.size(); i++) {
                    escrever(normal, 8.5f, TEXTO, descricao.get(i), x[2], base - i * 11);
                }
                aDireita(normal, 8.5f, TEXTO, String.valueOf(item.getQuantidade() != null ? item.getQuantidade() : 0), qtd, base);
                aDireita(normal, 8.5f, TEXTO, moeda(item.getValorUnitario()), unitario, base);
                aDireita(normal, 8.5f, TEXTO, moeda(subtotal(item)), subtotal, base);
                y -= altura;
                retaH(MARGEM, MARGEM + largura, y, LINHA, 0.6f);
            }
            if (y - 22 < RODAPE) {
                novaPagina(true);
            }
            preencher(MARGEM, y - 20, largura, 20, FUNDO);
            escrever(negrito, 8.5f, TEXTO, "Total do espelho", x[0], y - 13);
            aDireita(negrito, 8.5f, TEXTO, moeda(dados.espelho()), subtotal, y - 13);
            y -= 40;
        }

        private void cabecalhoTabela(float[] x, float qtd, float unitario, float subtotal) throws IOException {
            preencher(MARGEM, y - 20, largura, 20, FUNDO);
            escrever(negrito, 8.5f, TEXTO, "Tipo", x[0], y - 13);
            escrever(negrito, 8.5f, TEXTO, "TUSS", x[1], y - 13);
            escrever(negrito, 8.5f, TEXTO, "Descrição", x[2], y - 13);
            aDireita(negrito, 8.5f, TEXTO, "Qtd.", qtd, y - 13);
            aDireita(negrito, 8.5f, TEXTO, "Unitário", unitario, y - 13);
            aDireita(negrito, 8.5f, TEXTO, "Subtotal", subtotal, y - 13);
            y -= 20;
            retaH(MARGEM, MARGEM + largura, y, LINHA, 0.8f);
        }

        private void parecer() throws IOException {
            if (y - 50 < RODAPE) {
                novaPagina(true);
            }
            secao("Parecer");
            List<Glosa> glosas = dados.glosas();
            if (glosas.isEmpty()) {
                if (dados.semPrecoContratado()) {
                    paragrafo(normal, 9, CINZA, "Preço contratado não cadastrado para os procedimentos desta guia. "
                            + "Valor no espelho: " + moeda(dados.espelho())
                            + ". A conferência de valores precisa ser feita manualmente.");
                    return;
                }
                BigDecimal diferenca = dados.diferenca();
                paragrafo(normal, 9, CINZA, diferenca.signum() == 0
                        ? "Valores conferidos. Nenhuma glosa registrada."
                        : "Divergência de " + moeda(diferenca.abs()) + " entre o valor contratado e o espelho. Nenhuma glosa registrada.");
                return;
            }
            int numero = 1;
            for (Glosa glosa : glosas) {
                if (y - 50 < RODAPE) {
                    novaPagina(true);
                }
                String titulo = "Glosa " + numero++ + " · TUSS " + texto(glosa.getCodigoTuss())
                        + " · Valor revisado " + moeda(glosa.getValorGlosa())
                        + (glosa.getDataRegistro() != null ? " · " + DATA_HORA.format(glosa.getDataRegistro()) : "");
                paragrafo(negrito, 9, TEXTO, titulo);
                paragrafo(normal, 9, TEXTO, "Justificativa: " + texto(glosa.getJustificativaMilitar()));
                if (glosa.getConclusao() != null && !glosa.getConclusao().isBlank()) {
                    paragrafo(normal, 9, TEXTO, "Conclusão: " + glosa.getConclusao().trim());
                }
                y -= 8;
            }
        }

        private void rodape() throws IOException {
            float base = MARGEM + 62;
            float coluna = (largura - 30) / 2;
            float direita = MARGEM + coluna + 30;
            retaH(MARGEM, MARGEM + coluna, base, TEXTO, 0.9f);
            retaH(direita, MARGEM + largura, base, TEXTO, 0.9f);
            Glosa ultima = dados.ultimaGlosa();
            String auditor = ultima != null && ultima.getAuditor() != null ? ultima.getAuditor() : " ";
            centralizado(normal, 9, TEXTO, ajustar(normal, 9, auditor, coluna), MARGEM + coluna / 2, base - 13);
            centralizado(normal, 7, CINZA, "Auditoria de Lisura", MARGEM + coluna / 2, base - 23);
            centralizado(normal, 9, TEXTO, "Visto do Chefe da Seção", direita + coluna / 2, base - 13);
            centralizado(normal, 7, CINZA, "Data: ____/____/______", direita + coluna / 2, base - 23);
            retaH(MARGEM, MARGEM + largura, MARGEM + 18, LINHA, 0.8f);
            escrever(normal, 7, CINZA, dados.fechamentoId() != null ? "Fechamento #" + dados.fechamentoId() : "Fechamento -",
                    MARGEM, MARGEM + 6);
            aDireita(normal, 7, CINZA, "Gerado pelo sistema em " + GERADO_EM.format(LocalDateTime.now()),
                    MARGEM + largura, MARGEM + 6);
        }

        private void secao(String titulo) throws IOException {
            escrever(negrito, 9.5f, AZUL, titulo, MARGEM, y);
            y -= 7;
            retaH(MARGEM, MARGEM + largura, y, AZUL, 1.4f);
            y -= 14;
        }

        private void paragrafo(PDType1Font fonte, float corpo, Color cor, String valor) throws IOException {
            for (String parte : seguro(fonte, valor).split("\n")) {
                for (String linha : quebrar(fonte, corpo, parte, largura)) {
                    if (y - corpo < RODAPE) {
                        novaPagina(true);
                    }
                    escrever(fonte, corpo, cor, linha, MARGEM, y - corpo + 2);
                    y -= corpo + 4;
                }
            }
        }

        private void novaPagina(boolean continuacao) throws IOException {
            if (conteudo != null) {
                conteudo.close();
            }
            PDPage pagina = new PDPage(PDRectangle.A4);
            documento.addPage(pagina);
            conteudo = new PDPageContentStream(documento, pagina);
            y = tamanho.getHeight() - MARGEM - 8;
            if (continuacao) {
                escrever(negrito, 8, CINZA, "Relatório de Lisura · guia nº " + dados.numeroGuia() + " (continuação)", MARGEM, y);
                y -= 24;
            }
        }

        private void escrever(PDType1Font fonte, float corpo, Color cor, String valor, float x, float base) throws IOException {
            conteudo.beginText();
            conteudo.setFont(fonte, corpo);
            conteudo.setNonStrokingColor(cor);
            conteudo.newLineAtOffset(x, base);
            conteudo.showText(seguro(fonte, valor));
            conteudo.endText();
        }

        private void aDireita(PDType1Font fonte, float corpo, Color cor, String valor, float direita, float base) throws IOException {
            escrever(fonte, corpo, cor, valor, direita - medir(fonte, corpo, valor), base);
        }

        private void centralizado(PDType1Font fonte, float corpo, Color cor, String valor, float centro, float base) throws IOException {
            escrever(fonte, corpo, cor, valor, centro - medir(fonte, corpo, valor) / 2, base);
        }

        private void retaH(float x1, float x2, float altura, Color cor, float espessura) throws IOException {
            conteudo.setStrokingColor(cor);
            conteudo.setLineWidth(espessura);
            conteudo.moveTo(x1, altura);
            conteudo.lineTo(x2, altura);
            conteudo.stroke();
        }

        private void retaV(float x, float y1, float y2, Color cor, float espessura) throws IOException {
            conteudo.setStrokingColor(cor);
            conteudo.setLineWidth(espessura);
            conteudo.moveTo(x, y1);
            conteudo.lineTo(x, y2);
            conteudo.stroke();
        }

        private void preencher(float x, float base, float w, float h, Color cor) throws IOException {
            conteudo.setNonStrokingColor(cor);
            conteudo.addRect(x, base, w, h);
            conteudo.fill();
        }

        private float medir(PDType1Font fonte, float corpo, String valor) throws IOException {
            return fonte.getStringWidth(seguro(fonte, valor)) / 1000 * corpo;
        }

        private String ajustar(PDType1Font fonte, float corpo, String valor, float maximo) throws IOException {
            String atual = seguro(fonte, valor);
            if (medir(fonte, corpo, atual) <= maximo) {
                return atual;
            }
            while (atual.length() > 1 && medir(fonte, corpo, atual + "...") > maximo) {
                atual = atual.substring(0, atual.length() - 1);
            }
            return atual.trim() + "...";
        }

        private List<String> quebrar(PDType1Font fonte, float corpo, String paragrafo, float maximo) throws IOException {
            List<String> linhas = new ArrayList<>();
            StringBuilder atual = new StringBuilder();
            for (String palavra : seguro(fonte, paragrafo).split(" ")) {
                String tentativa = atual.isEmpty() ? palavra : atual + " " + palavra;
                if (medir(fonte, corpo, tentativa) <= maximo || atual.isEmpty()) {
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
            for (char c : (valor == null ? "" : valor).replace("\r", "").replace('\t', ' ').toCharArray()) {
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
    }
}
