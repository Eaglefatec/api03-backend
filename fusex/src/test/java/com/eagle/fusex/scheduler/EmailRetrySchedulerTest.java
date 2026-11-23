package com.eagle.fusex.scheduler;

import com.eagle.fusex.model.dto.ResultadoEnvioEmail;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.enums.StatusEmail;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.service.SolicitacaoEmailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class EmailRetrySchedulerTest {

    @Mock
    private SolicitacaoMedicaRepository solicitacaoRepository;

    @Mock
    private SolicitacaoEmailService emailService;

    @InjectMocks
    private EmailRetryScheduler scheduler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Deve reprocessar solicitacao pendente com sucesso e atualizar status para ENVIADO")
    void deveReprocessarComSucesso() {
        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        solicitacao.setId(100L);
        solicitacao.setStatusEmail(StatusEmail.PENDENTE_REENVIO);
        solicitacao.setEmailTentativas(1);
        solicitacao.setEmailProximaTentativa(LocalDateTime.now().minusMinutes(1));
        solicitacao.setPaciente(new Paciente("Fulano", "12345678901", "123", "OM1", "fulano@email.com"));

        when(solicitacaoRepository.findByStatusEmailAndEmailProximaTentativaLessThanEqualAndEmailTentativasLessThan(
                eq(StatusEmail.PENDENTE_REENVIO), any(), eq(EmailRetryScheduler.MAX_TENTATIVAS)))
                .thenReturn(List.of(solicitacao));

        when(emailService.enviarEmailSolicitacao(solicitacao))
                .thenReturn(ResultadoEnvioEmail.sucesso("fulano@email.com"));

        scheduler.processarFilaReenvioEmail();

        assertEquals(StatusEmail.ENVIADO, solicitacao.getStatusEmail());
        assertNull(solicitacao.getEmailProximaTentativa());
        assertEquals(2, solicitacao.getEmailTentativas());
        verify(solicitacaoRepository).save(solicitacao);
    }

    @Test
    @DisplayName("Deve incrementar tentativas e agendar proxima execucao em caso de nova falha temporaria")
    void deveAgendarProximaExecucaoEmNovaFalha() {
        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        solicitacao.setId(101L);
        solicitacao.setStatusEmail(StatusEmail.PENDENTE_REENVIO);
        solicitacao.setEmailTentativas(2);
        solicitacao.setEmailProximaTentativa(LocalDateTime.now().minusMinutes(1));
        solicitacao.setPaciente(new Paciente("Ciclano", "12345678901", "124", "OM1", "ciclano@email.com"));

        when(solicitacaoRepository.findByStatusEmailAndEmailProximaTentativaLessThanEqualAndEmailTentativasLessThan(
                eq(StatusEmail.PENDENTE_REENVIO), any(), eq(EmailRetryScheduler.MAX_TENTATIVAS)))
                .thenReturn(List.of(solicitacao));

        when(emailService.enviarEmailSolicitacao(solicitacao))
                .thenReturn(ResultadoEnvioEmail.falhaConexao("Timeout de conexao SMTP"));

        scheduler.processarFilaReenvioEmail();

        assertEquals(StatusEmail.PENDENTE_REENVIO, solicitacao.getStatusEmail());
        assertNotNull(solicitacao.getEmailProximaTentativa());
        assertEquals(3, solicitacao.getEmailTentativas());
        verify(solicitacaoRepository).save(solicitacao);
    }

    @Test
    @DisplayName("Deve marcar como FALHA_ENVIO quando atingir o limite maximo de tentativas")
    void deveMarcarFalhaEnvioAoExcederLimite() {
        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        solicitacao.setId(102L);
        solicitacao.setStatusEmail(StatusEmail.PENDENTE_REENVIO);
        solicitacao.setEmailTentativas(4);
        solicitacao.setEmailProximaTentativa(LocalDateTime.now().minusMinutes(1));
        solicitacao.setPaciente(new Paciente("Beltrano", "12345678901", "125", "OM1", "beltrano@email.com"));

        when(solicitacaoRepository.findByStatusEmailAndEmailProximaTentativaLessThanEqualAndEmailTentativasLessThan(
                eq(StatusEmail.PENDENTE_REENVIO), any(), eq(EmailRetryScheduler.MAX_TENTATIVAS)))
                .thenReturn(List.of(solicitacao));

        when(emailService.enviarEmailSolicitacao(solicitacao))
                .thenReturn(ResultadoEnvioEmail.falhaConexao("Servidor fora do ar"));

        scheduler.processarFilaReenvioEmail();

        assertEquals(StatusEmail.FALHA_ENVIO, solicitacao.getStatusEmail());
        assertNull(solicitacao.getEmailProximaTentativa());
        assertEquals(5, solicitacao.getEmailTentativas());
        verify(solicitacaoRepository).save(solicitacao);
    }
}

