package com.eagle.fusex.loader;

import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.repository.OcsProcedimentoRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.repository.ProcedimentoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class OcsPrecoLoaderTest {

    @Mock
    private OcsRepository ocsRepository;

    @Mock
    private ProcedimentoRepository procedimentoRepository;

    @Mock
    private OcsProcedimentoRepository ocsProcedimentoRepository;

    private OcsPrecoLoader loader;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        loader = new OcsPrecoLoader(ocsRepository, procedimentoRepository, ocsProcedimentoRepository,
                "data/ocs_precos.csv", "src/test/resources/data/ocs_precos.csv");
    }

    @Test
    void testRun_ImportaOcsEPrecosDoCsvDeFixture() throws Exception {
        when(ocsProcedimentoRepository.count()).thenReturn(0L);
        when(ocsRepository.findByOcsNome(anyString())).thenReturn(Optional.empty());
        when(ocsRepository.save(any(Ocs.class))).thenAnswer(invocation -> invocation.getArgument(0));

        when(procedimentoRepository.findById("10101012"))
                .thenReturn(Optional.of(new Procedimento("10101012", "Consulta em consultorio", 1)));
        when(procedimentoRepository.findById("LOCAL-000001")).thenReturn(Optional.empty());
        when(procedimentoRepository.save(any(Procedimento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        loader.run();

        verify(ocsProcedimentoRepository, times(2)).save(any());
        verify(ocsRepository, times(1)).save(any(Ocs.class));
    }

    @Test
    void testRun_JaTemDados_NaoImportaDeNovo() throws Exception {
        when(ocsProcedimentoRepository.count()).thenReturn(10L);

        loader.run();

        verify(ocsRepository, never()).save(any());
        verify(ocsProcedimentoRepository, never()).save(any());
    }
}
