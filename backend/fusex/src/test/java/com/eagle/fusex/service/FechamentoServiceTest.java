package com.eagle.fusex.service;

import com.eagle.fusex.exception.FechamentoJaEnviadoException;
import com.eagle.fusex.exception.ValidationException;
import com.eagle.fusex.model.dto.response.FechamentoResponse;
import com.eagle.fusex.model.entity.Lote;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.repository.FechamentoRepository;
import com.eagle.fusex.repository.GuiaRepository;
import com.eagle.fusex.repository.LoteRepository;
import com.eagle.fusex.repository.OcsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FechamentoServiceTest {

    @Mock
    private FechamentoRepository fechamentoRepository;

    @Mock
    private LoteRepository loteRepository;

    @Mock
    private GuiaRepository guiaRepository;

    @Mock
    private OcsRepository ocsRepository;

    private FechamentoService service;
    private Ocs ocsLogada;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new FechamentoService(fechamentoRepository, loteRepository, guiaRepository, ocsRepository);

        ocsLogada = new Ocs();
        ocsLogada.setOcsId(1L);
        ocsLogada.setLoginUsuario("ocs");

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("ocs", null, List.of()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testEnviarFechamento_ComLotesPendentes_CriaFechamento() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.of(ocsLogada));
        when(fechamentoRepository.existsByOcs_OcsIdAndCompetenciaAnoAndCompetenciaMes(1L, 2026, 10)).thenReturn(false);

        Lote lote = new Lote();
        lote.setId(5L);
        when(loteRepository.findByOcs_OcsIdAndCompetenciaAnoAndCompetenciaMesAndFechamentoIsNull(1L, 2026, 10))
                .thenReturn(List.of(lote));
        when(fechamentoRepository.save(any())).thenAnswer(inv -> {
            var f = inv.getArgument(0, com.eagle.fusex.model.entity.Fechamento.class);
            f.setId(100L);
            return f;
        });
        when(guiaRepository.countByLoteId(5L)).thenReturn(3L);

        FechamentoResponse response = service.enviarFechamento(2026, 10);

        assertEquals(1, response.getQuantidadeLotes());
        assertEquals(3L, response.getQuantidadeGuias());
        assertEquals(100L, lote.getFechamento().getId());
    }

    @Test
    void testEnviarFechamento_CompetenciaJaEnviada_LancaExcecao() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.of(ocsLogada));
        when(fechamentoRepository.existsByOcs_OcsIdAndCompetenciaAnoAndCompetenciaMes(1L, 2026, 10)).thenReturn(true);

        assertThrows(FechamentoJaEnviadoException.class, () -> service.enviarFechamento(2026, 10));
    }

    @Test
    void testEnviarFechamento_SemLotesNaCompetencia_LancaExcecao() {
        when(ocsRepository.findByLoginUsuario("ocs")).thenReturn(Optional.of(ocsLogada));
        when(fechamentoRepository.existsByOcs_OcsIdAndCompetenciaAnoAndCompetenciaMes(1L, 2026, 10)).thenReturn(false);
        when(loteRepository.findByOcs_OcsIdAndCompetenciaAnoAndCompetenciaMesAndFechamentoIsNull(1L, 2026, 10))
                .thenReturn(List.of());

        assertThrows(ValidationException.class, () -> service.enviarFechamento(2026, 10));
    }
}
