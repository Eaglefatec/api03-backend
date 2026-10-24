package com.eagle.fusex.service;

import com.eagle.fusex.model.dto.response.GuiaValidadeResponse;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.entity.TriagemPreGuia;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.repository.TriagemPreGuiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class GuiaValidadeServiceTest {

    @Mock
    private SolicitacaoMedicaRepository solicitacaoRepository;

    @Mock
    private TriagemPreGuiaRepository triagemRepository;

    private GuiaValidadeService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new GuiaValidadeService(solicitacaoRepository, triagemRepository);
    }

    @Test
    void testConsultarValidade_NumeroNaoEncontrado() {
        when(solicitacaoRepository.findByNumeroGuia("ABC")).thenReturn(Optional.empty());

        GuiaValidadeResponse response = service.consultarValidade("ABC");

        assertFalse(response.isValida());
        assertEquals("NUMERO_NAO_ENCONTRADO", response.getMotivoInvalidez());
    }

    @Test
    void testConsultarValidade_GuiaValida() {
        SolicitacaoMedica solicitacao = montarSolicitacao(true, LocalDate.now().plusDays(10));
        when(solicitacaoRepository.findByNumeroGuia("ABC")).thenReturn(Optional.of(solicitacao));
        when(triagemRepository.findBySolicitacaoMedicaId(solicitacao.getId())).thenReturn(Optional.empty());

        GuiaValidadeResponse response = service.consultarValidade("ABC");

        assertTrue(response.isValida());
        assertNull(response.getMotivoInvalidez());
    }

    @Test
    void testConsultarValidade_GuiaSubstituida() {
        SolicitacaoMedica solicitacao = montarSolicitacao(false, LocalDate.now().plusDays(10));
        when(solicitacaoRepository.findByNumeroGuia("ABC")).thenReturn(Optional.of(solicitacao));
        when(triagemRepository.findBySolicitacaoMedicaId(solicitacao.getId())).thenReturn(Optional.empty());

        GuiaValidadeResponse response = service.consultarValidade("ABC");

        assertFalse(response.isValida());
        assertEquals("GUIA_SUBSTITUIDA", response.getMotivoInvalidez());
    }

    @Test
    void testConsultarValidade_GuiaExpirada() {
        SolicitacaoMedica solicitacao = montarSolicitacao(true, LocalDate.now().minusDays(1));
        when(solicitacaoRepository.findByNumeroGuia("ABC")).thenReturn(Optional.of(solicitacao));
        when(triagemRepository.findBySolicitacaoMedicaId(solicitacao.getId())).thenReturn(Optional.empty());

        GuiaValidadeResponse response = service.consultarValidade("ABC");

        assertFalse(response.isValida());
        assertEquals("GUIA_EXPIRADA", response.getMotivoInvalidez());
    }

    @Test
    void testConsultarValidade_RetornaOcsVinculadaQuandoTriagemExiste() {
        SolicitacaoMedica solicitacao = montarSolicitacao(true, LocalDate.now().plusDays(10));
        Ocs ocs = new Ocs();
        ocs.setOcsNome("Clínica Teste");
        TriagemPreGuia triagem = new TriagemPreGuia();
        triagem.setOcs(ocs);

        when(solicitacaoRepository.findByNumeroGuia("ABC")).thenReturn(Optional.of(solicitacao));
        when(triagemRepository.findBySolicitacaoMedicaId(solicitacao.getId())).thenReturn(Optional.of(triagem));

        GuiaValidadeResponse response = service.consultarValidade("ABC");

        assertEquals("Clínica Teste", response.getOcsVinculada());
    }

    private SolicitacaoMedica montarSolicitacao(boolean valida, LocalDate dataValidade) {
        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        solicitacao.setId(1L);
        solicitacao.setValida(valida);
        solicitacao.setDataValidade(dataValidade);
        Paciente paciente = new Paciente();
        paciente.setNome("Paciente Teste");
        solicitacao.setPaciente(paciente);
        return solicitacao;
    }
}
