package com.eagle.fusex.service;

import com.eagle.fusex.exception.BusinessException;
import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.model.dto.response.PreGuiaResumoResponse;
import com.eagle.fusex.model.entity.Encaminhamento;
import com.eagle.fusex.model.entity.GuiaAssinatura;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.entity.TriagemPreGuia;
import com.eagle.fusex.model.enums.StatusEsteiraAssinatura;
import com.eagle.fusex.model.enums.StatusPreGuia;
import com.eagle.fusex.repository.EncaminhamentoRepository;
import com.eagle.fusex.repository.GuiaAssinaturaRepository;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.repository.TriagemPreGuiaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PreGuiaServiceTest {

    @Mock private TriagemPreGuiaRepository triagemRepository;
    @Mock private EncaminhamentoRepository encaminhamentoRepository;
    @Mock private SolicitacaoMedicaRepository solicitacaoRepository;
    @Mock private GuiaAssinaturaRepository guiaAssinaturaRepository;
    @Mock private GuiaAssinaturaService guiaAssinaturaService;

    private PreGuiaService service;
    private SolicitacaoMedica pendente;
    private SolicitacaoMedica emitida;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PreGuiaService(triagemRepository, encaminhamentoRepository, solicitacaoRepository,
                guiaAssinaturaRepository, guiaAssinaturaService);

        Ocs ocs = new Ocs();
        ocs.setOcsNome("CLINICA SANTA LUCIA");

        pendente = solicitacao(1L, "token-1", "MARIA DE OLIVEIRA", null);
        emitida = solicitacao(2L, "token-2", "PAULO NOGUEIRA", "202611038");
        emitida.setDataConfirmacaoEmissao(LocalDateTime.of(2026, 11, 11, 10, 0));

        when(triagemRepository.findPreGuiasDeSolicitacoesValidas())
                .thenReturn(List.of(triagem(pendente, ocs), triagem(emitida, ocs)));

        Encaminhamento encaminhamento = new Encaminhamento();
        encaminhamento.setSolicitacaoMedica(pendente);
        when(encaminhamentoRepository.findBySolicitacaoMedicaIdIn(anyCollection())).thenReturn(List.of(encaminhamento));
    }

    private SolicitacaoMedica solicitacao(Long id, String token, String nome, String numeroGuia) {
        Paciente paciente = new Paciente();
        paciente.setNome(nome);
        paciente.setPrec("33333333333");
        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        solicitacao.setId(id);
        solicitacao.setTokenPublico(token);
        solicitacao.setPaciente(paciente);
        solicitacao.setNumeroGuia(numeroGuia);
        return solicitacao;
    }

    private TriagemPreGuia triagem(SolicitacaoMedica solicitacao, Ocs ocs) {
        TriagemPreGuia triagem = new TriagemPreGuia();
        triagem.setSolicitacaoMedica(solicitacao);
        triagem.setOcs(ocs);
        triagem.setDataCriacao(LocalDateTime.of(2026, 11, 10, 14, 30));
        return triagem;
    }

    @Test
    void testListar_Pendentes_TrazSomenteSemGuiaComIndicacaoDeEncaminhamento() {
        List<PreGuiaResumoResponse> resultado = service.listar(StatusPreGuia.PENDENTE);

        assertEquals(1, resultado.size());
        PreGuiaResumoResponse resumo = resultado.get(0);
        assertEquals("token-1", resumo.token());
        assertEquals("MARIA DE OLIVEIRA", resumo.beneficiarioNome());
        assertEquals("CLINICA SANTA LUCIA", resumo.ocsNome());
        assertNull(resumo.numeroGuia());
        assertTrue(resumo.possuiEncaminhamento());
    }

    @Test
    void testListar_Emitidas_TrazNumeroEDataDeEmissao() {
        List<PreGuiaResumoResponse> resultado = service.listar(StatusPreGuia.EMITIDA);

        assertEquals(1, resultado.size());
        assertEquals(LocalDateTime.of(2026, 11, 11, 10, 0), resultado.get(0).dataEmissao());
        assertFalse(resultado.get(0).possuiEncaminhamento());
    }

    @Test
    void testListar_SemFiltro_TrazTodas() {
        assertEquals(2, service.listar(null).size());
    }

    @Test
    void testAbrirEncaminhamento_SemEncaminhamento_LancaExcecao() {
        when(solicitacaoRepository.findByTokenPublico("token-2")).thenReturn(Optional.of(emitida));
        when(encaminhamentoRepository.findFirstBySolicitacaoMedicaIdOrderByDataHoraDesc(2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.abrirEncaminhamento("token-2"));
    }

    @Test
    void testListar_Liberadas_SomenteConfirmadasAindaNaoEnviadas() {
        assertEquals(List.of("token-2"), service.listar(StatusPreGuia.LIBERADA).stream().map(PreGuiaResumoResponse::token).toList());

        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setSolicitacaoMedica(emitida);
        guia.setNumeroGuia("202610904");
        guia.setStatus(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);
        guia.setDataEnvio(LocalDateTime.now());
        when(guiaAssinaturaRepository.findBySolicitacaoMedicaIdIn(anyCollection())).thenReturn(List.of(guia));

        assertTrue(service.listar(StatusPreGuia.LIBERADA).isEmpty());
        PreGuiaResumoResponse emitidaNaEsteira = service.listar(StatusPreGuia.EMITIDA).get(0);
        assertEquals("202610904", emitidaNaEsteira.numeroGuia());
        assertEquals("Aguardando Médico Chefe", emitidaNaEsteira.situacaoAssinatura());
        assertTrue(emitidaNaEsteira.enviadaParaAssinatura());
    }

    @Test
    void testConfirmarEmissao_ComEncaminhamento_RegistraData() {
        when(solicitacaoRepository.findByTokenPublico("token-1")).thenReturn(Optional.of(pendente));
        when(triagemRepository.findBySolicitacaoMedicaId(1L)).thenReturn(Optional.of(triagem(pendente, new Ocs())));
        when(encaminhamentoRepository.existsBySolicitacaoMedicaId(1L)).thenReturn(true);

        PreGuiaResumoResponse resumo = service.confirmarEmissao("token-1");

        assertNotNull(pendente.getDataConfirmacaoEmissao());
        assertNotNull(resumo.dataEmissao());
        verify(solicitacaoRepository).save(pendente);
    }

    @Test
    void testConfirmarEmissao_SemEncaminhamento_ConfirmaMesmoAssim() {
        when(solicitacaoRepository.findByTokenPublico("token-1")).thenReturn(Optional.of(pendente));
        when(triagemRepository.findBySolicitacaoMedicaId(1L)).thenReturn(Optional.of(triagem(pendente, new Ocs())));
        when(encaminhamentoRepository.existsBySolicitacaoMedicaId(1L)).thenReturn(false);

        PreGuiaResumoResponse resumo = service.confirmarEmissao("token-1");

        assertNotNull(pendente.getDataConfirmacaoEmissao());
        assertFalse(resumo.possuiEncaminhamento());
    }

    @Test
    void testConfirmarEmissao_JaConfirmada_LancaExcecao() {
        when(solicitacaoRepository.findByTokenPublico("token-2")).thenReturn(Optional.of(emitida));
        when(triagemRepository.findBySolicitacaoMedicaId(2L)).thenReturn(Optional.of(triagem(emitida, new Ocs())));

        assertThrows(BusinessException.class, () -> service.confirmarEmissao("token-2"));
    }

    @Test
    void testEnviarParaAssinatura_SemConfirmacao_LancaExcecao() {
        when(solicitacaoRepository.findByTokenPublico("token-1")).thenReturn(Optional.of(pendente));
        when(triagemRepository.findBySolicitacaoMedicaId(1L)).thenReturn(Optional.of(triagem(pendente, new Ocs())));
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1});

        BusinessException erro = assertThrows(BusinessException.class, () -> service.enviarParaAssinatura("token-1", arquivo));
        assertTrue(erro.getMessage().contains("Confirme a emissão"));
        verifyNoInteractions(guiaAssinaturaService);
    }

    @Test
    void testEnviarParaAssinatura_JaEnviada_LancaExcecao() {
        when(solicitacaoRepository.findByTokenPublico("token-2")).thenReturn(Optional.of(emitida));
        when(triagemRepository.findBySolicitacaoMedicaId(2L)).thenReturn(Optional.of(triagem(emitida, new Ocs())));
        when(guiaAssinaturaRepository.existsBySolicitacaoMedicaIdAndStatusNot(2L, StatusEsteiraAssinatura.REJEITADA)).thenReturn(true);
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1});

        assertThrows(BusinessException.class, () -> service.enviarParaAssinatura("token-2", arquivo));
        verifyNoInteractions(guiaAssinaturaService);
    }

    @Test
    void testEnviarParaAssinatura_Confirmada_EnviaComDadosDaPreGuia() {
        Ocs ocs = new Ocs();
        ocs.setOcsNome("CLINICA SANTA LUCIA");
        emitida.getPaciente().setEmail("paulo@fusex.eb.mil.br");
        when(solicitacaoRepository.findByTokenPublico("token-2")).thenReturn(Optional.of(emitida));
        when(triagemRepository.findBySolicitacaoMedicaId(2L)).thenReturn(Optional.of(triagem(emitida, ocs)));
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1});

        service.enviarParaAssinatura("token-2", arquivo);

        ArgumentCaptor<GuiaAssinaturaService.DadosPreGuia> captor = ArgumentCaptor.forClass(GuiaAssinaturaService.DadosPreGuia.class);
        verify(guiaAssinaturaService).enviarGuiaParaAssinatura(eq(arquivo), isNull(), captor.capture());
        assertSame(emitida, captor.getValue().solicitacao());
        assertEquals("PAULO NOGUEIRA", captor.getValue().beneficiarioNome());
        assertEquals("paulo@fusex.eb.mil.br", captor.getValue().beneficiarioEmail());
        assertEquals("CLINICA SANTA LUCIA", captor.getValue().ocsNome());
    }
}
