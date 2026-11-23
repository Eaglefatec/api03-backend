package com.eagle.fusex.service;

import com.eagle.fusex.model.dto.ResultadoEnvioEmail;
import com.eagle.fusex.model.entity.Medico;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.enums.StatusEmail;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SolicitacaoEmailServiceTest {

    @Test
    @DisplayName("Não deve disparar envio real quando mail.enabled for false")
    void deveSimularEnvioQuandoDesabilitado() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        QrCodeService qrCodeService = new QrCodeService("http://localhost:8080/api");
        SolicitacaoEmailService service = new SolicitacaoEmailService(provider, qrCodeService, false, "noreply@fusex.eb.mil.br");

        Paciente paciente = new Paciente("João", "12345678901", "12345", "OM-01", "joao@email.com");
        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        solicitacao.setPaciente(paciente);
        solicitacao.setTokenPublico("token-xyz");

        ResultadoEnvioEmail resultado = service.enviarEmailSolicitacao(solicitacao);
        assertNotNull(resultado);
        assertFalse(resultado.sucesso());
        assertEquals(StatusEmail.SIMULADO, resultado.status());
        verifyNoInteractions(provider);
    }

    @Test
    @DisplayName("Deve enviar e-mail com link direto e QR Code inline quando mail.enabled for true")
    void deveEnviarEmailQuandoHabilitado() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        QrCodeService qrCodeService = new QrCodeService("http://localhost:8080/api");
        SolicitacaoEmailService service = new SolicitacaoEmailService(provider, qrCodeService, true, "noreply@fusex.eb.mil.br");

        Medico medico = new Medico();
        medico.setNome("Dr. Carlos");

        Paciente paciente = new Paciente("Maria", "98765432100", "54321", "OM-02", "maria@email.com");
        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        solicitacao.setMedico(medico);
        solicitacao.setPaciente(paciente);
        solicitacao.setTokenPublico("token-abc-123");

        ResultadoEnvioEmail resultado = service.enviarEmailSolicitacao(solicitacao);
        assertNotNull(resultado);
        assertTrue(resultado.sucesso());
        assertEquals(StatusEmail.ENVIADO, resultado.status());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Deve classificar erro de rede/SMTP como PENDENTE_REENVIO retentável")
    void deveRetornarPendenteReenvioQuandoHouverFalhaConexao() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("Connection refused")).when(mailSender).send(any(MimeMessage.class));

        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        QrCodeService qrCodeService = new QrCodeService("http://localhost:8080/api");
        SolicitacaoEmailService service = new SolicitacaoEmailService(provider, qrCodeService, true, "noreply@fusex.eb.mil.br");

        Paciente paciente = new Paciente("Maria", "98765432100", "54321", "OM-02", "maria@email.com");
        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        solicitacao.setPaciente(paciente);
        solicitacao.setTokenPublico("token-abc-123");

        ResultadoEnvioEmail resultado = service.enviarEmailSolicitacao(solicitacao);
        assertNotNull(resultado);
        assertFalse(resultado.sucesso());
        assertEquals(StatusEmail.PENDENTE_REENVIO, resultado.status());
        assertTrue(resultado.retentavel());
    }

    @Test
    @DisplayName("Deve classificar e-mail inválido como FALHA_ENVIO não retentável")
    void deveRetornarFalhaInvalidoQuandoEmailInvalido() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        QrCodeService qrCodeService = new QrCodeService("http://localhost:8080/api");
        SolicitacaoEmailService service = new SolicitacaoEmailService(provider, qrCodeService, true, "noreply@fusex.eb.mil.br");

        Paciente paciente = new Paciente("Maria", "98765432100", "54321", "OM-02", "email-sem-arroba");
        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        solicitacao.setPaciente(paciente);
        solicitacao.setTokenPublico("token-abc-123");

        ResultadoEnvioEmail resultado = service.enviarEmailSolicitacao(solicitacao);
        assertNotNull(resultado);
        assertFalse(resultado.sucesso());
        assertEquals(StatusEmail.FALHA_ENVIO, resultado.status());
        assertFalse(resultado.retentavel());
    }
}
