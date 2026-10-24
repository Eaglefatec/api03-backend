package com.eagle.fusex.service;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.exception.OcsInvalidoException;
import com.eagle.fusex.model.dto.response.GuiaResponse;
import com.eagle.fusex.model.entity.Guia;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.enums.StatusGuia;
import com.eagle.fusex.repository.GuiaRepository;
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

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GuiaServiceTest {

    @Mock
    private GuiaRepository guiaRepository;

    @Mock
    private OcsRepository ocsRepository;

    @Mock
    private FileStorageService fileStorageService;

    private GuiaService service;
    private Ocs ocsLogada;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new GuiaService(guiaRepository, ocsRepository, fileStorageService);

        ocsLogada = new Ocs();
        ocsLogada.setOcsId(1L);
        ocsLogada.setOcsNome("Plani Diagnósticos Médicos Ltda");
        ocsLogada.setLoginUsuario("ocs");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ocs", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testAnexarGuia_ComDadosValidos_SalvaGuia() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.of(ocsLogada));
        when(guiaRepository.existsByNumeroGuia("202610606")).thenReturn(false);
        when(fileStorageService.salvar(any(MultipartFile.class), anyString())).thenReturn("uploads/guia.pdf");
        when(guiaRepository.save(any(Guia.class))).thenAnswer(inv -> inv.getArgument(0));

        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1, 2, 3});

        GuiaResponse response = service.anexarGuia("202610606", "34596112300", LocalDate.of(2026, 10, 2), arquivo);

        assertEquals("202610606", response.getNumeroGuia());
        assertEquals(StatusGuia.SEM_LOTE, response.getStatus());
    }

    @Test
    void testAnexarGuia_NumeroGuiaDuplicado_LancaExcecao() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.of(ocsLogada));
        when(guiaRepository.existsByNumeroGuia("202610606")).thenReturn(true);

        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThrows(ArquivoInvalidoException.class, () ->
                service.anexarGuia("202610606", "34596112300", LocalDate.of(2026, 10, 2), arquivo));
    }

    @Test
    void testAnexarGuia_ArquivoNaoPdf_LancaExcecao() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.of(ocsLogada));

        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.txt", "text/plain", "conteudo".getBytes());

        assertThrows(ArquivoInvalidoException.class, () ->
                service.anexarGuia("202610606", "34596112300", LocalDate.of(2026, 10, 2), arquivo));
    }

    @Test
    void testAnexarGuia_OcsNaoAssociadaAoUsuario_LancaExcecao() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.empty());

        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThrows(OcsInvalidoException.class, () ->
                service.anexarGuia("202610606", "34596112300", LocalDate.of(2026, 10, 2), arquivo));
    }

    @Test
    void testListarGuias_RetornaGuiasDaOcsLogada() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.of(ocsLogada));

        Guia guia = new Guia();
        guia.setId(1L);
        guia.setNumeroGuia("202610606");
        guia.setBeneficiarioPrecCp("34596112300");
        guia.setDataAtendimento(LocalDate.of(2026, 10, 2));
        guia.setNomeArquivo("guia.pdf");
        guia.setStatus(StatusGuia.SEM_LOTE);

        when(guiaRepository.findByOcs_LoginUsuarioOrderByCreatedAtDesc("ocs")).thenReturn(List.of(guia));

        List<GuiaResponse> resultado = service.listarGuias();

        assertEquals(1, resultado.size());
        assertEquals("202610606", resultado.get(0).getNumeroGuia());
    }
}
