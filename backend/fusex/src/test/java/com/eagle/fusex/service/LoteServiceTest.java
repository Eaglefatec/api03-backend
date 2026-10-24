package com.eagle.fusex.service;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.exception.ValidationException;
import com.eagle.fusex.model.dto.response.LoteResponse;
import com.eagle.fusex.model.entity.Guia;
import com.eagle.fusex.model.entity.Lote;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.enums.StatusGuia;
import com.eagle.fusex.repository.GuiaRepository;
import com.eagle.fusex.repository.LoteRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.storage.FileStorageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoteServiceTest {

    @Mock
    private LoteRepository loteRepository;

    @Mock
    private GuiaRepository guiaRepository;

    @Mock
    private OcsRepository ocsRepository;

    @Mock
    private FileStorageService fileStorageService;

    private LoteService service;
    private Ocs ocsLogada;
    private Ocs outraOcs;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new LoteService(loteRepository, guiaRepository, ocsRepository, fileStorageService);

        ocsLogada = new Ocs();
        ocsLogada.setOcsId(1L);
        ocsLogada.setLoginUsuario("ocs");

        outraOcs = new Ocs();
        outraOcs.setOcsId(2L);

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ocs", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private Guia criarGuia(Long id, Ocs ocs, StatusGuia status) {
        Guia guia = new Guia();
        guia.setId(id);
        guia.setOcs(ocs);
        guia.setNumeroGuia("G-" + id);
        guia.setStatus(status);
        return guia;
    }

    @Test
    void testGerarLote_ComGuiasValidas_CriaLoteEAtualizaStatus() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.of(ocsLogada));
        Guia guia1 = criarGuia(1L, ocsLogada, StatusGuia.SEM_LOTE);
        Guia guia2 = criarGuia(2L, ocsLogada, StatusGuia.SEM_LOTE);
        when(guiaRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of(guia1, guia2));
        when(loteRepository.countByOcs_OcsIdAndCompetenciaAnoAndCompetenciaMes(eq(1L), anyInt(), anyInt())).thenReturn(0L);
        when(fileStorageService.salvar(any(MultipartFile.class), anyString())).thenReturn("uploads/arquivo");
        when(loteRepository.save(any(Lote.class))).thenAnswer(inv -> {
            Lote l = inv.getArgument(0);
            l.setId(10L);
            return l;
        });
        when(guiaRepository.countByLoteId(10L)).thenReturn(2L);

        MockMultipartFile fatura = new MockMultipartFile("fatura", "fatura.pdf", "application/pdf", new byte[]{1});
        MockMultipartFile espelho = new MockMultipartFile("espelho", "espelho.xlsx", "application/vnd.ms-excel", new byte[]{1});

        LoteResponse response = service.gerarLote(List.of(1L, 2L), fatura, espelho);

        assertEquals(1, response.getNumeroLote());
        assertEquals(2L, response.getQuantidadeGuias());
        assertEquals(StatusGuia.EM_LOTE, guia1.getStatus());
        assertEquals(StatusGuia.EM_LOTE, guia2.getStatus());
        assertEquals(10L, guia1.getLote().getId());
    }

    @Test
    void testGerarLote_SemGuiasSelecionadas_LancaExcecao() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.of(ocsLogada));
        MockMultipartFile fatura = new MockMultipartFile("fatura", "fatura.pdf", "application/pdf", new byte[]{1});
        MockMultipartFile espelho = new MockMultipartFile("espelho", "espelho.pdf", "application/pdf", new byte[]{1});

        assertThrows(ValidationException.class, () -> service.gerarLote(List.of(), fatura, espelho));
    }

    @Test
    void testGerarLote_GuiaDeOutraOcs_LancaExcecao() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.of(ocsLogada));
        Guia guiaDeOutraOcs = criarGuia(1L, outraOcs, StatusGuia.SEM_LOTE);
        when(guiaRepository.findAllById(List.of(1L))).thenReturn(List.of(guiaDeOutraOcs));

        MockMultipartFile fatura = new MockMultipartFile("fatura", "fatura.pdf", "application/pdf", new byte[]{1});
        MockMultipartFile espelho = new MockMultipartFile("espelho", "espelho.pdf", "application/pdf", new byte[]{1});

        assertThrows(ValidationException.class, () -> service.gerarLote(List.of(1L), fatura, espelho));
    }

    @Test
    void testGerarLote_GuiaJaEmLote_LancaExcecao() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.of(ocsLogada));
        Guia guiaEmLote = criarGuia(1L, ocsLogada, StatusGuia.EM_LOTE);
        when(guiaRepository.findAllById(List.of(1L))).thenReturn(List.of(guiaEmLote));

        MockMultipartFile fatura = new MockMultipartFile("fatura", "fatura.pdf", "application/pdf", new byte[]{1});
        MockMultipartFile espelho = new MockMultipartFile("espelho", "espelho.pdf", "application/pdf", new byte[]{1});

        assertThrows(ValidationException.class, () -> service.gerarLote(List.of(1L), fatura, espelho));
    }

    @Test
    void testGerarLote_GuiaInexistente_LancaExcecao() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.of(ocsLogada));
        when(guiaRepository.findAllById(List.of(99L))).thenReturn(List.of());

        MockMultipartFile fatura = new MockMultipartFile("fatura", "fatura.pdf", "application/pdf", new byte[]{1});
        MockMultipartFile espelho = new MockMultipartFile("espelho", "espelho.pdf", "application/pdf", new byte[]{1});

        assertThrows(ResourceNotFoundException.class, () -> service.gerarLote(List.of(99L), fatura, espelho));
    }

    @Test
    void testGerarLote_FaturaForaDoFormatoPdf_LancaExcecao() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.of(ocsLogada));
        Guia guia = criarGuia(1L, ocsLogada, StatusGuia.SEM_LOTE);
        when(guiaRepository.findAllById(List.of(1L))).thenReturn(List.of(guia));

        MockMultipartFile fatura = new MockMultipartFile("fatura", "fatura.docx", "application/octet-stream", new byte[]{1});
        MockMultipartFile espelho = new MockMultipartFile("espelho", "espelho.pdf", "application/pdf", new byte[]{1});

        assertThrows(ArquivoInvalidoException.class, () -> service.gerarLote(List.of(1L), fatura, espelho));
    }
}
