package com.eagle.fusex.loader;

import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.repository.ProcedimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TussProcedimentoLoaderTest {

    @Mock
    private ProcedimentoRepository procedimentoRepository;

    private TussProcedimentoLoader loader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loader = new TussProcedimentoLoader(procedimentoRepository, "data/tuss_procedimentos.csv",
                "src/test/resources/data/tuss_procedimentos.csv");
    }

    @Test
    void testRun_ImportaProcedimentosDoCsvDeFixture() throws Exception {
        when(procedimentoRepository.count()).thenReturn(0L);

        loader.run();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Procedimento>> captor = ArgumentCaptor.forClass(List.class);
        verify(procedimentoRepository).saveAll(captor.capture());

        List<Procedimento> importados = captor.getValue();
        assertEquals(3, importados.size());
        assertEquals("10101012", importados.get(0).getProcCodigoDgp());
        assertEquals("Consulta em consultorio", importados.get(0).getProcDescricao());
    }

    @Test
    void testRun_JaTemDados_NaoImportaDeNovo() throws Exception {
        when(procedimentoRepository.count()).thenReturn(5L);

        loader.run();

        verify(procedimentoRepository, never()).saveAll(any());
    }
}
