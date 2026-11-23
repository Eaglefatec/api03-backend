package com.eagle.fusex.service;

import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.entity.Glosa;
import com.eagle.fusex.model.entity.Guia;
import com.eagle.fusex.model.entity.GuiaItem;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.repository.GlosaRepository;
import com.eagle.fusex.repository.GuiaItemRepository;
import com.eagle.fusex.repository.GuiaRepository;
import com.eagle.fusex.repository.PacienteRepository;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GuiaRelatorioLisuraPdfServiceTest {

    @TempDir
    Path pasta;

    @Mock private GuiaRepository guiaRepository;
    @Mock private GlosaRepository glosaRepository;
    @Mock private GuiaItemRepository guiaItemRepository;
    @Mock private PacienteRepository pacienteRepository;

    private GuiaRelatorioLisuraPdfService service;
    private Guia guia;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        service = new GuiaRelatorioLisuraPdfService(guiaRepository, glosaRepository, guiaItemRepository, pacienteRepository);

        Path arquivo = pasta.resolve("guia.pdf");
        try (PDDocument original = new PDDocument()) {
            original.addPage(new PDPage(PDRectangle.A4));
            original.save(arquivo.toFile());
        }

        Ocs ocs = new Ocs();
        ocs.setOcsNome("CARDIOVITA SERVICOS MEDICOS");
        guia = new Guia();
        guia.setId(20L);
        guia.setNumeroGuia("202610700");
        guia.setBeneficiarioNome("MARIA DE OLIVEIRA");
        guia.setBeneficiarioPrecCp("33333333333");
        guia.setOcs(ocs);
        guia.setDataAtendimento(LocalDate.of(2026, 11, 10));
        guia.setValorContratado(new BigDecimal("150.00"));
        guia.setValorFaturado(new BigDecimal("200.00"));
        guia.setCaminhoArquivo(arquivo.toString());

        when(guiaRepository.findById(20L)).thenReturn(Optional.of(guia));
        when(guiaItemRepository.findByGuiaId(20L)).thenReturn(List.of());
        when(glosaRepository.findByGuiaId(20L)).thenReturn(List.of());
    }

    private String textoDaPagina(byte[] pdf, int pagina) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setStartPage(pagina);
            stripper.setEndPage(pagina);
            return stripper.getText(documento);
        }
    }

    private int paginas(byte[] pdf) throws IOException {
        try (PDDocument documento = Loader.loadPDF(pdf)) {
            return documento.getNumberOfPages();
        }
    }

    @Test
    void testGerar_ComGlosa_RelatorioNoVersoDaGuia() throws IOException {
        Glosa glosa = new Glosa();
        glosa.setGuia(guia);
        glosa.setCodigoTuss("40301010");
        glosa.setJustificativaMilitar("Valor acima da tabela contratada");
        glosa.setConclusao("Glosa parcial aplicada");
        glosa.setValorGlosa(new BigDecimal("150.00"));
        glosa.setAuditor("admin@gmail.com");
        glosa.setDataRegistro(LocalDateTime.of(2026, 11, 19, 9, 30));
        when(glosaRepository.findByGuiaId(20L)).thenReturn(List.of(glosa));

        ArquivoLoteDownload arquivo = service.gerarGuiaComRelatorio(20L);

        assertEquals("application/pdf", arquivo.tipo());
        assertEquals(2, paginas(arquivo.conteudo()));
        String verso = textoDaPagina(arquivo.conteudo(), 2);
        assertTrue(verso.contains("Relatório de Lisura"));
        assertTrue(verso.contains("Verso da guia nº 202610700"));
        assertTrue(verso.contains("Glosa 1"));
        assertTrue(verso.contains("Valor acima da tabela contratada"));
        assertTrue(verso.contains("Glosa parcial aplicada"));
        assertTrue(verso.contains("admin@gmail.com"));
    }

    @Test
    void testGerar_SemGlosa_InformaQueNaoHouveObservacao() throws IOException {
        String verso = textoDaPagina(service.gerarGuiaComRelatorio(20L).conteudo(), 2);

        assertTrue(verso.contains("Nenhuma glosa registrada"));
        assertTrue(verso.contains("Divergência de R$ 50,00"));
    }

    @Test
    void testGerar_SemPrecoContratado_NaoApresentaComoConferido() throws IOException {
        guia.setValorContratado(BigDecimal.ZERO);
        GuiaItem item = new GuiaItem();
        item.setCodigoTuss("40301842");
        item.setDescricaoItem("Ferro sérico, dosagem");
        item.setTipo("Procedimento");
        item.setQuantidade(1);
        item.setValorUnitario(new BigDecimal("11.25"));
        when(guiaItemRepository.findByGuiaId(20L)).thenReturn(List.of(item));

        String verso = textoDaPagina(service.gerarGuiaComRelatorio(20L).conteudo(), 2);

        assertTrue(verso.contains("Não identificado"));
        assertTrue(verso.contains("Não calculada"));
        assertTrue(verso.contains("Valor contratado não identificado na guia"));
        assertFalse(verso.contains("Valores conferidos"));
        assertFalse(verso.contains("Sem divergência"));
    }

    @Test
    void testGerar_ValorDoEspelhoSomaOsItens() throws IOException {
        GuiaItem item = new GuiaItem();
        item.setCodigoTuss("10101012");
        item.setDescricaoItem("Consulta eletiva");
        item.setTipo("Procedimento");
        item.setQuantidade(2);
        item.setValorUnitario(new BigDecimal("75.00"));
        when(guiaItemRepository.findByGuiaId(20L)).thenReturn(List.of(item));

        String verso = textoDaPagina(service.gerarGuiaComRelatorio(20L).conteudo(), 2);

        assertTrue(verso.contains("Sem divergência"));
        assertTrue(verso.contains("Valores conferidos. Nenhuma glosa registrada."));
        assertTrue(verso.contains("Consulta eletiva"));
    }

    @Test
    void testGerar_JustificativaLonga_QuebraEmVariasLinhas() throws IOException {
        Glosa glosa = new Glosa();
        glosa.setJustificativaMilitar("Procedimento sem autorizacao previa ".repeat(40));
        glosa.setValorGlosa(BigDecimal.ZERO);
        when(glosaRepository.findByGuiaId(20L)).thenReturn(List.of(glosa));

        byte[] pdf = service.gerarGuiaComRelatorio(20L).conteudo();

        assertTrue(paginas(pdf) >= 2);
        assertTrue(textoDaPagina(pdf, 2).contains("Procedimento sem autorizacao previa"));
    }

    @Test
    void testGerar_ArquivoDaGuiaInexistente_LancaExcecao() {
        guia.setCaminhoArquivo(pasta.resolve("nao-existe.pdf").toString());

        assertThrows(ResourceNotFoundException.class, () -> service.gerarGuiaComRelatorio(20L));
    }

    @Test
    void testGerar_GuiaInexistente_LancaExcecao() {
        when(guiaRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.gerarGuiaComRelatorio(99L));
    }

    @Test
    void testGerar_SemNomeNaGuia_BuscaBeneficiarioPeloPrec() throws IOException {
        guia.setBeneficiarioNome(null);
        Paciente paciente = new Paciente();
        paciente.setNome("ROSANA TICIANELLI PINTO");
        when(pacienteRepository.findByPrec("33333333333")).thenReturn(Optional.of(paciente));

        assertTrue(textoDaPagina(service.gerarGuiaComRelatorio(20L).conteudo(), 2).contains("ROSANA TICIANELLI PINTO"));
    }
}
