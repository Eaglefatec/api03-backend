package com.eagle.fusex.service;

import com.eagle.fusex.exception.OcsInvalidoException;
import com.eagle.fusex.exception.SolicitacaoNaoEncontradaException;
import com.eagle.fusex.exception.TriagemJaPreenchidaException;
import com.eagle.fusex.model.dto.request.PreencherTriagemRequest;
import com.eagle.fusex.model.dto.response.SolicitacaoPublicaResponse;
import com.eagle.fusex.model.dto.response.TriagemResponse;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.entity.TriagemPreGuia;
import com.eagle.fusex.model.enums.Especialidade;
import com.eagle.fusex.repository.OcsProcedimentoRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.repository.PacienteRepository;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.repository.SolicitacaoProcedimentoRepository;
import com.eagle.fusex.repository.TriagemPreGuiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

class TriagemPreGuiaServiceTest {

    @Mock
    private SolicitacaoMedicaRepository solicitacaoRepository;

    @Mock
    private TriagemPreGuiaRepository triagemRepository;

    @Mock
    private OcsRepository ocsRepository;

    @Mock
    private SolicitacaoProcedimentoRepository solicitacaoProcedimentoRepository;

    @Mock
    private OcsProcedimentoRepository ocsProcedimentoRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private TriagemPreGuiaService service;

    private SolicitacaoMedica solicitacao;
    private Paciente paciente;
    private Ocs ocs;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        paciente = new Paciente("João Silva", "12345678901", "OM001");
        paciente.setId(1L);

        solicitacao = new SolicitacaoMedica();
        solicitacao.setId(1L);
        solicitacao.setTokenPublico("token-uuid-123");
        solicitacao.setValida(true);
        solicitacao.setPaciente(paciente);

        Set<Especialidade> especialidades = new HashSet<>();
        especialidades.add(Especialidade.CARDIOLOGISTA);
        solicitacao.setEspecialidades(especialidades);

        ocs = new Ocs();
        ocs.setOcsId(1L);
        ocs.setOcsNome("Clínica Teste");

        when(solicitacaoProcedimentoRepository.findBySolicitacaoMedicaId(anyLong()))
                .thenReturn(Collections.emptyList());
    }

    @Test
    void testBuscarPorToken_TokenInexistente_LancaException() {
        when(solicitacaoRepository.findByTokenPublico("token-invalido"))
                .thenReturn(Optional.empty());

        assertThrows(SolicitacaoNaoEncontradaException.class,
                () -> service.buscarPorToken("token-invalido"));
    }

    @Test
    void testBuscarPorToken_TokenInvalido_LancaException() {
        SolicitacaoMedica solicitacaoInvalida = new SolicitacaoMedica();
        solicitacaoInvalida.setTokenPublico("token-invalido");
        solicitacaoInvalida.setValida(false);

        when(solicitacaoRepository.findByTokenPublico("token-invalido"))
                .thenReturn(Optional.of(solicitacaoInvalida));

        assertThrows(SolicitacaoNaoEncontradaException.class,
                () -> service.buscarPorToken("token-invalido"));
    }

    @Test
    void testBuscarPorToken_TokenValido_RetornaSolicitacao() {
        when(solicitacaoRepository.findByTokenPublico("token-uuid-123"))
                .thenReturn(Optional.of(solicitacao));
        when(triagemRepository.findBySolicitacaoMedicaId(1L))
                .thenReturn(Optional.empty());

        SolicitacaoPublicaResponse response = service.buscarPorToken("token-uuid-123");

        assertNotNull(response);
        assertEquals("João Silva", response.getNomePaciente());
        assertEquals("OM001", response.getOm());
        assertFalse(response.getTriagemPreenchida());
    }

    @Test
    void testBuscarPorToken_ComTriagemPreenchida_RetornaIndicador() {
        TriagemPreGuia triagem = new TriagemPreGuia();
        triagem.setId(1L);

        when(solicitacaoRepository.findByTokenPublico("token-uuid-123"))
                .thenReturn(Optional.of(solicitacao));
        when(triagemRepository.findBySolicitacaoMedicaId(1L))
                .thenReturn(Optional.of(triagem));

        SolicitacaoPublicaResponse response = service.buscarPorToken("token-uuid-123");

        assertNotNull(response);
        assertTrue(response.getTriagemPreenchida());
    }

    @Test
    void testPreencherTriagem_TokenInexistente_LancaException() {
        when(solicitacaoRepository.findByTokenPublico("token-invalido"))
                .thenReturn(Optional.empty());

        PreencherTriagemRequest request = new PreencherTriagemRequest();
        request.setCpfPrec("12345678901");
        request.setIdade(30);
        request.setTelefone("11999999999");
        request.setOcsId(1L);
        request.setAceitoTermos(true);

        assertThrows(SolicitacaoNaoEncontradaException.class,
                () -> service.preencherTriagem("token-invalido", request));
    }

    @Test
    void testPreencherTriagem_TriagemJaPreenchida_LancaException() {
        TriagemPreGuia triagemExistente = new TriagemPreGuia();
        triagemExistente.setId(1L);

        when(solicitacaoRepository.findByTokenPublico("token-uuid-123"))
                .thenReturn(Optional.of(solicitacao));
        when(triagemRepository.findBySolicitacaoMedicaId(1L))
                .thenReturn(Optional.of(triagemExistente));

        PreencherTriagemRequest request = new PreencherTriagemRequest();
        request.setCpfPrec("12345678901");
        request.setIdade(30);
        request.setTelefone("11999999999");
        request.setOcsId(1L);
        request.setAceitoTermos(true);

        assertThrows(TriagemJaPreenchidaException.class,
                () -> service.preencherTriagem("token-uuid-123", request));
    }

    @Test
    void testPreencherTriagem_OcsInexistente_LancaException() {
        when(solicitacaoRepository.findByTokenPublico("token-uuid-123"))
                .thenReturn(Optional.of(solicitacao));
        when(triagemRepository.findBySolicitacaoMedicaId(1L))
                .thenReturn(Optional.empty());
        when(ocsRepository.findById(99L))
                .thenReturn(Optional.empty());

        PreencherTriagemRequest request = new PreencherTriagemRequest();
        request.setCpfPrec("12345678901");
        request.setIdade(30);
        request.setTelefone("11999999999");
        request.setOcsId(99L);
        request.setAceitoTermos(true);

        assertThrows(OcsInvalidoException.class,
                () -> service.preencherTriagem("token-uuid-123", request));
    }

    @Test
    void testPreencherTriagem_ComSucesso_RetornaOpcoes() {
        when(solicitacaoRepository.findByTokenPublico("token-uuid-123"))
                .thenReturn(Optional.of(solicitacao));
        when(triagemRepository.findBySolicitacaoMedicaId(1L))
                .thenReturn(Optional.empty());
        when(ocsRepository.findById(1L))
                .thenReturn(Optional.of(ocs));

        PreencherTriagemRequest request = new PreencherTriagemRequest();
        request.setCpfPrec("12345678901");
        request.setIdade(30);
        request.setTelefone("11999999999");
        request.setOcsId(1L);
        request.setAceitoTermos(true);

        TriagemResponse response = service.preencherTriagem("token-uuid-123", request);

        assertNotNull(response);
        assertNotNull(response.getOpcoes());
        assertEquals(2, response.getOpcoes().size());
        assertTrue(response.getOpcoes().stream()
                .anyMatch(o -> "PRESENCIAL".equals(o.getTipo())));
        assertTrue(response.getOpcoes().stream()
                .anyMatch(o -> "PRE_GUIA_DIGITAL".equals(o.getTipo())));

        verify(triagemRepository).save(any(TriagemPreGuia.class));
    }
}
