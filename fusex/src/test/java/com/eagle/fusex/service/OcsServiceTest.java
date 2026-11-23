package com.eagle.fusex.service;

import com.eagle.fusex.model.dto.response.OcsResponse;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.repository.OcsProcedimentoRepository;
import com.eagle.fusex.repository.OcsRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class OcsServiceTest {

    @Mock
    private OcsRepository ocsRepository;

    @Mock
    private OcsProcedimentoRepository ocsProcedimentoRepository;

    @InjectMocks
    private OcsService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Deve retornar lista vazia se nenhum procedimento for informado")
    void deveRetornarVazioSemProcedimentos() {
        List<OcsResponse> resultado = service.listarPorProcedimentos(Collections.emptyList());
        assertNotNull(resultado);
        assertTrue(resultado.isEmpty());
        verifyNoInteractions(ocsRepository);
    }

    @Test
    @DisplayName("Deve filtrar apenas OCS que cobrem todos os procedimentos solicitados")
    void deveFiltrarOcsComCoberturaIntegral() {
        List<String> codigos = List.of("DGP001", "DGP002");

        Ocs ocsApta = new Ocs();
        ocsApta.setOcsId(10L);
        ocsApta.setOcsNome("Hospital Militar Central");

        when(ocsRepository.findOcsQueAtendemTodosProcedimentos(codigos, 2L))
                .thenReturn(List.of(ocsApta));

        List<OcsResponse> resultado = service.listarPorProcedimentos(codigos);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("Hospital Militar Central", resultado.get(0).getOcsNome());
        verify(ocsRepository, times(1)).findOcsQueAtendemTodosProcedimentos(codigos, 2L);
    }
}

