package com.eagle.fusex.service;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.exception.MedicoInvalidoException;
import com.eagle.fusex.model.dto.response.EncaminhamentoResponse;
import com.eagle.fusex.model.entity.Encaminhamento;
import com.eagle.fusex.model.entity.Medico;
import com.eagle.fusex.repository.EncaminhamentoRepository;
import com.eagle.fusex.repository.MedicoRepository;
import com.eagle.fusex.storage.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class EncaminhamentoServiceTest {

    @Mock
    private MedicoRepository medicoRepository;

    @Mock
    private EncaminhamentoRepository encaminhamentoRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private MultipartFile arquivo;

    private EncaminhamentoService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new EncaminhamentoService(medicoRepository, encaminhamentoRepository, fileStorageService);
    }

    @Test
    void testEnviarEncaminhamentoComSucesso() {
        Medico medico = new Medico("Dr. João", "123456", true);
        when(medicoRepository.findByIdAndCredenciadoTrue(1L)).thenReturn(Optional.of(medico));
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getContentType()).thenReturn("application/pdf");
        when(arquivo.getOriginalFilename()).thenReturn("exame.pdf");
        when(arquivo.getSize()).thenReturn(1024L);
        when(fileStorageService.salvar(eq(arquivo), anyString())).thenReturn("uploads/mocked-uuid.pdf");

        Encaminhamento salvo = new Encaminhamento();
        salvo.setId(10L);
        salvo.setMedico(medico);
        when(encaminhamentoRepository.save(any(Encaminhamento.class))).thenReturn(salvo);

        EncaminhamentoResponse response = service.enviarEncaminhamento(1L, arquivo);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Encaminhamento recebido com sucesso", response.getMensagem());
        verify(fileStorageService, times(1)).salvar(eq(arquivo), anyString());
        verify(encaminhamentoRepository, times(1)).save(any(Encaminhamento.class));
    }

    @Test
    void testEnviarEncaminhamentoComMedicoNaoCredenciado() {
        when(medicoRepository.findByIdAndCredenciadoTrue(1L)).thenReturn(Optional.empty());

        assertThrows(MedicoInvalidoException.class, () -> {
            service.enviarEncaminhamento(1L, arquivo);
        });

        verify(medicoRepository, times(1)).findByIdAndCredenciadoTrue(1L);
    }

    @Test
    void testEnviarEncaminhamentoComArquivoVazio() {
        Medico medico = new Medico("Dr. João", "123456", true);
        when(medicoRepository.findByIdAndCredenciadoTrue(1L)).thenReturn(Optional.of(medico));
        when(arquivo.isEmpty()).thenReturn(true);

        assertThrows(ArquivoInvalidoException.class, () -> {
            service.enviarEncaminhamento(1L, arquivo);
        });
    }

    @Test
    void testEnviarEncaminhamentoComTipoArquivoInvalido() {
        Medico medico = new Medico("Dr. João", "123456", true);
        when(medicoRepository.findByIdAndCredenciadoTrue(1L)).thenReturn(Optional.of(medico));
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getContentType()).thenReturn("text/plain");
        when(arquivo.getOriginalFilename()).thenReturn("documento.txt");

        assertThrows(ArquivoInvalidoException.class, () -> {
            service.enviarEncaminhamento(1L, arquivo);
        });
    }

    @Test
    void testEnviarEncaminhamentoComArquivoMaiorQue6MB() {
        Medico medico = new Medico("Dr. João", "123456", true);
        when(medicoRepository.findByIdAndCredenciadoTrue(1L)).thenReturn(Optional.of(medico));
        when(arquivo.isEmpty()).thenReturn(false);
        when(arquivo.getContentType()).thenReturn("application/pdf");
        when(arquivo.getOriginalFilename()).thenReturn("documento.pdf");
        when(arquivo.getSize()).thenReturn(7 * 1024 * 1024L);

        assertThrows(ArquivoInvalidoException.class, () -> {
            service.enviarEncaminhamento(1L, arquivo);
        });
    }
}
