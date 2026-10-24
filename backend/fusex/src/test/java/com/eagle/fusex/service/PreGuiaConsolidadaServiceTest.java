package com.eagle.fusex.service;

import com.eagle.fusex.exception.SolicitacaoNaoEncontradaException;
import com.eagle.fusex.exception.TriagemNaoPreenchidaException;
import com.eagle.fusex.model.dto.response.PreGuiaConsolidadaResponse;
import com.eagle.fusex.model.entity.Medico;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.OcsProcedimento;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.entity.SolicitacaoProcedimento;
import com.eagle.fusex.model.entity.TriagemPreGuia;
import com.eagle.fusex.repository.OcsProcedimentoRepository;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.repository.SolicitacaoProcedimentoRepository;
import com.eagle.fusex.repository.TriagemPreGuiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

class PreGuiaConsolidadaServiceTest {

    @Mock
    private SolicitacaoMedicaRepository solicitacaoRepository;

    @Mock
    private TriagemPreGuiaRepository triagemRepository;

    @Mock
    private SolicitacaoProcedimentoRepository solicitacaoProcedimentoRepository;

    @Mock
    private OcsProcedimentoRepository ocsProcedimentoRepository;

    @InjectMocks
    private PreGuiaConsolidadaService service;

    private SolicitacaoMedica solicitacao;
    private Paciente paciente;
    private Medico medicoEmissor;
    private TriagemPreGuia triagem;
    private Ocs ocs;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        paciente = new Paciente("João Silva", "12345678901", "OM001");
        paciente.setId(1L);

        medicoEmissor = new Medico("Dr. João Silva", "123456", true);
        medicoEmissor.setId(1L);

        ocs = new Ocs();
        ocs.setOcsId(1L);
        ocs.setOcsNome("Clínica Teste");

        solicitacao = new SolicitacaoMedica();
        solicitacao.setId(1L);
        solicitacao.setTokenPublico("token-uuid-123");
        solicitacao.setValida(true);
        solicitacao.setPaciente(paciente);
        solicitacao.setMedico(medicoEmissor);
        solicitacao.setDataCriacao(LocalDateTime.now());

        triagem = new TriagemPreGuia();
        triagem.setId(1L);
        triagem.setIdade(30);
        triagem.setCpfPrec("12345678901");
        triagem.setTelefone("11999999999");
        triagem.setOcs(ocs);
    }

    @Test
    void testConsultar_TokenInexistente_LancaException() {
        when(solicitacaoRepository.findByTokenPublico("token-invalido"))
                .thenReturn(Optional.empty());

        assertThrows(SolicitacaoNaoEncontradaException.class,
                () -> service.consultar("token-invalido"));
    }

    @Test
    void testConsultar_TriagemNaoPreenchida_LancaException() {
        when(solicitacaoRepository.findByTokenPublico("token-uuid-123"))
                .thenReturn(Optional.of(solicitacao));
        when(triagemRepository.findBySolicitacaoMedicaId(1L))
                .thenReturn(Optional.empty());

        assertThrows(TriagemNaoPreenchidaException.class,
                () -> service.consultar("token-uuid-123"));
    }

    @Test
    void testConsultar_ComSucesso_MedicoResponsavelNulo() {
        when(solicitacaoRepository.findByTokenPublico("token-uuid-123"))
                .thenReturn(Optional.of(solicitacao));
        when(triagemRepository.findBySolicitacaoMedicaId(1L))
                .thenReturn(Optional.of(triagem));
        when(solicitacaoProcedimentoRepository.findBySolicitacaoMedicaId(1L))
                .thenReturn(List.of());

        PreGuiaConsolidadaResponse response = service.consultar("token-uuid-123");

        assertNotNull(response);
        assertEquals("João Silva", response.getBeneficiario().getNome());
        assertEquals(30, response.getBeneficiario().getIdade());
        assertEquals("Clínica Teste", response.getOcs().getOcsNome());
        assertEquals("Dr. João Silva", response.getMedicoEmissor().getNome());
        assertNull(response.getMedicoResponsavel());
        assertTrue(response.getProcedimentos().isEmpty());
    }

    @Test
    void testConsultar_ComSucesso_MultiplosProcedimentosEPreco() {
        Procedimento procedimento1 = new Procedimento("DGP001", "Consulta eletiva", 1);
        Procedimento procedimento2 = new Procedimento("DGP002", "ECG", 1);

        SolicitacaoProcedimento vinculo1 = new SolicitacaoProcedimento();
        vinculo1.setProcedimento(procedimento1);
        vinculo1.setQuantidade(1);
        vinculo1.setValorNoMomento(new BigDecimal("60.00"));

        SolicitacaoProcedimento vinculo2 = new SolicitacaoProcedimento();
        vinculo2.setProcedimento(procedimento2);
        vinculo2.setQuantidade(2);
        vinculo2.setValorNoMomento(null);

        OcsProcedimento op2 = new OcsProcedimento();
        op2.setValor(new BigDecimal("21.00"));

        when(solicitacaoRepository.findByTokenPublico("token-uuid-123"))
                .thenReturn(Optional.of(solicitacao));
        when(triagemRepository.findBySolicitacaoMedicaId(1L))
                .thenReturn(Optional.of(triagem));
        when(solicitacaoProcedimentoRepository.findBySolicitacaoMedicaId(1L))
                .thenReturn(List.of(vinculo1, vinculo2));
        when(ocsProcedimentoRepository.findByOcs_OcsIdAndProcedimento_ProcCodigoDgp(1L, "DGP002"))
                .thenReturn(Optional.of(op2));

        PreGuiaConsolidadaResponse response = service.consultar("token-uuid-123");

        assertNotNull(response);
        assertEquals(2, response.getProcedimentos().size());
        assertEquals("DGP001", response.getProcedimentos().get(0).getProcedimentoCodigoDgp());
        assertEquals(new BigDecimal("60.00"), response.getProcedimentos().get(0).getValor());
        assertEquals("DGP002", response.getProcedimentos().get(1).getProcedimentoCodigoDgp());
        assertEquals(new BigDecimal("21.00"), response.getProcedimentos().get(1).getValor());
    }
}
