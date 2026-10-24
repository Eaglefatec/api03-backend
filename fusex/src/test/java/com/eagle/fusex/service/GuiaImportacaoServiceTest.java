package com.eagle.fusex.service;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.exception.SolicitacaoNaoEncontradaException;
import com.eagle.fusex.model.dto.response.GuiaImportacaoResponse;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.storage.FileStorageService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class GuiaImportacaoServiceTest {

    private static final String REGEX = "(?i)guia\\s+de\\s+encaminhamento\\s+n[ºr°.]*\\s*:?\\s*([A-Z0-9./-]{4,})";

    @Mock
    private SolicitacaoMedicaRepository solicitacaoRepository;

    @Mock
    private FileStorageService fileStorageService;

    private GuiaImportacaoService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new GuiaImportacaoService(solicitacaoRepository, fileStorageService, REGEX, 60);
    }

    private byte[] gerarPdf(String texto) throws IOException {
        try (PDDocument documento = new PDDocument()) {
            PDPage pagina = new PDPage();
            documento.addPage(pagina);
            try (PDPageContentStream stream = new PDPageContentStream(documento, pagina)) {
                stream.beginText();
                stream.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 12);
                stream.newLineAtOffset(50, 700);
                stream.showText(texto);
                stream.endText();
            }
            ByteArrayOutputStream saida = new ByteArrayOutputStream();
            documento.save(saida);
            return saida.toByteArray();
        }
    }

    @Test
    void testImportarPdf_ComNumeroReconhecivel_ExtraiNumeroGuia() throws Exception {
        byte[] pdf = gerarPdf("GUIA DE ENCAMINHAMENTO Nr: 202610904 / FUSEX");
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", pdf);

        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        when(solicitacaoRepository.findByTokenPublico("token-1")).thenReturn(Optional.of(solicitacao));
        when(fileStorageService.salvar(eq(arquivo), anyString())).thenReturn("uploads/guia.pdf");
        when(solicitacaoRepository.save(any(SolicitacaoMedica.class))).thenAnswer(inv -> inv.getArgument(0));

        GuiaImportacaoResponse response = service.importarPdf("token-1", arquivo);

        assertTrue(response.isNumeroIdentificado());
        assertEquals("202610904", response.getNumeroGuia());
        assertEquals("202610904", solicitacao.getNumeroGuia());
        assertNotNull(solicitacao.getDataImportacaoGuia());
        assertEquals(solicitacao.getDataImportacaoGuia().toLocalDate().plusDays(60), solicitacao.getDataValidade());
    }

    @Test
    void testImportarPdf_SemNumeroReconhecivel_SalvaComNumeroNulo() throws Exception {
        byte[] pdf = gerarPdf("Documento sem nenhum padrao reconhecivel");
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", pdf);

        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        when(solicitacaoRepository.findByTokenPublico("token-1")).thenReturn(Optional.of(solicitacao));
        when(fileStorageService.salvar(eq(arquivo), anyString())).thenReturn("uploads/guia.pdf");
        when(solicitacaoRepository.save(any(SolicitacaoMedica.class))).thenAnswer(inv -> inv.getArgument(0));

        GuiaImportacaoResponse response = service.importarPdf("token-1", arquivo);

        assertFalse(response.isNumeroIdentificado());
        assertNull(response.getNumeroGuia());
        assertNull(solicitacao.getNumeroGuia());
        assertNotNull(solicitacao.getDataImportacaoGuia());
    }

    @Test
    void testImportarPdf_SolicitacaoNaoEncontrada_LancaExcecao() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1, 2, 3});
        when(solicitacaoRepository.findByTokenPublico("token-x")).thenReturn(Optional.empty());

        assertThrows(SolicitacaoNaoEncontradaException.class, () -> service.importarPdf("token-x", arquivo));
    }

    @Test
    void testImportarPdf_ArquivoNaoPdf_LancaExcecao() {
        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        when(solicitacaoRepository.findByTokenPublico("token-1")).thenReturn(Optional.of(solicitacao));
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.txt", "text/plain", "conteudo".getBytes());

        assertThrows(ArquivoInvalidoException.class, () -> service.importarPdf("token-1", arquivo));
    }

    @Test
    void testCorrigirNumeroGuia_AtualizaNumeroManualmente() {
        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        when(solicitacaoRepository.findByTokenPublico("token-1")).thenReturn(Optional.of(solicitacao));
        when(solicitacaoRepository.save(any(SolicitacaoMedica.class))).thenAnswer(inv -> inv.getArgument(0));

        GuiaImportacaoResponse response = service.corrigirNumeroGuia("token-1", "XYZ-999");

        assertEquals("XYZ-999", response.getNumeroGuia());
        assertEquals("XYZ-999", solicitacao.getNumeroGuia());
    }
}
