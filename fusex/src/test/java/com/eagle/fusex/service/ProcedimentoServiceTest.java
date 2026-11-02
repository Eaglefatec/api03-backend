package com.eagle.fusex.service;

import com.eagle.fusex.model.dto.response.ProcedimentoResponse;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.model.enums.Especialidade;
import com.eagle.fusex.repository.ProcedimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProcedimentoServiceTest {

    @Mock
    private ProcedimentoRepository procedimentoRepository;

    @InjectMocks
    private ProcedimentoService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Deve buscar procedimentos sem especialidade usando busca geral")
    void deveBuscarSemEspecialidade() {
        Procedimento p1 = new Procedimento("DGP001", "Consulta Ortopédica", 1);
        Procedimento p2 = new Procedimento("DGP002", "Ecocardiograma", 1);

        when(procedimentoRepository.findTop20ByProcDescricaoContainingIgnoreCaseOrderByProcDescricao("Consulta"))
                .thenReturn(List.of(p1));

        List<ProcedimentoResponse> resultado = service.listarProcedimentos("Consulta", null);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("DGP001", resultado.get(0).procCodigoDgp());
        verify(procedimentoRepository, times(1))
                .findTop20ByProcDescricaoContainingIgnoreCaseOrderByProcDescricao("Consulta");
    }

    @Test
    @DisplayName("Deve buscar procedimentos filtrados por especialidade e excluir de outras especialidades")
    void deveBuscarFiltradoPorEspecialidade() {
        Procedimento pOrtopedia = new Procedimento("ORT001", "Imobilização de Fratura", 1);

        Set<Especialidade> especialidades = Set.of(Especialidade.ORTOPEDISTA);

        when(procedimentoRepository.findTop20ByDescricaoAndEspecialidades("Fratura", especialidades))
                .thenReturn(List.of(pOrtopedia));

        List<ProcedimentoResponse> resultado = service.listarProcedimentos("Fratura", especialidades);

        assertNotNull(resultado);
        assertEquals(1, resultado.size());
        assertEquals("ORT001", resultado.get(0).procCodigoDgp());
        assertEquals("Imobilização de Fratura", resultado.get(0).procDescricao());
        verify(procedimentoRepository, times(1))
                .findTop20ByDescricaoAndEspecialidades("Fratura", especialidades);
    }
}

