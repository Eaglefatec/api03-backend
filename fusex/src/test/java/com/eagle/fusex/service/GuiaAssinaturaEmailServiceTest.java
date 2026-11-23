package com.eagle.fusex.service;

import com.eagle.fusex.model.dto.ResultadoEnvioEmail;
import com.eagle.fusex.model.entity.GuiaAssinatura;
import com.eagle.fusex.model.enums.StatusEmail;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class GuiaAssinaturaEmailServiceTest {

    @Test
    @DisplayName("Deve simular envio de e-mail da guia assinada quando o envio de e-mail estiver desabilitado")
    void deveSimularEnvioQuandoDesabilitado() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        GuiaAssinaturaEmailService service = new GuiaAssinaturaEmailService(provider, false, "noreply@fusex.eb.mil.br");

        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setNumeroGuia("202610703");
        guia.setBeneficiarioNome("Luana P. Azevedo");
        guia.setBeneficiarioEmail("luana.azevedo@email.com.br");
        guia.setOcsNome("Oftalmo Vale S/S Ltda");
        guia.setDataValidade(LocalDate.now().plusDays(60));

        ResultadoEnvioEmail resultado = service.enviarEmailGuiaAssinada(guia, new byte[]{1, 2, 3}, "Mensagem pronta");

        assertNotNull(resultado);
        assertFalse(resultado.sucesso());
        assertEquals(StatusEmail.SIMULADO, resultado.status());
    }

    @Test
    @DisplayName("Deve enviar e-mail com anexo PDF da guia assinada quando habilitado")
    void deveEnviarEmailComAnexoQuandoHabilitado() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);

        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        GuiaAssinaturaEmailService service = new GuiaAssinaturaEmailService(provider, true, "noreply@fusex.eb.mil.br");

        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setNumeroGuia("202610703");
        guia.setBeneficiarioNome("Luana P. Azevedo");
        guia.setBeneficiarioEmail("luana.azevedo@email.com.br");
        guia.setOcsNome("Oftalmo Vale S/S Ltda");
        guia.setDataValidade(LocalDate.now().plusDays(60));

        ResultadoEnvioEmail resultado = service.enviarEmailGuiaAssinada(guia, new byte[]{10, 20, 30}, "Mensagem pronta de teste");

        assertNotNull(resultado);
        assertTrue(resultado.sucesso());
        assertEquals(StatusEmail.ENVIADO, resultado.status());
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }

    @Test
    @DisplayName("Deve classificar erro SMTP como PENDENTE_REENVIO")
    void deveRetornarPendenteReenvioQuandoFalhaConexaoSmtp() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        MimeMessage mimeMessage = new MimeMessage(Session.getInstance(new Properties()));
        when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        doThrow(new MailSendException("SMTP timeout")).when(mailSender).send(any(MimeMessage.class));

        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(mailSender);

        GuiaAssinaturaEmailService service = new GuiaAssinaturaEmailService(provider, true, "noreply@fusex.eb.mil.br");

        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setNumeroGuia("202610703");
        guia.setBeneficiarioNome("Luana P. Azevedo");
        guia.setBeneficiarioEmail("luana.azevedo@email.com.br");
        guia.setOcsNome("Oftalmo Vale S/S Ltda");

        ResultadoEnvioEmail resultado = service.enviarEmailGuiaAssinada(guia, new byte[]{1}, "Mensagem pronta");

        assertNotNull(resultado);
        assertFalse(resultado.sucesso());
        assertEquals(StatusEmail.PENDENTE_REENVIO, resultado.status());
        assertTrue(resultado.retentavel());
    }

    @Test
    @DisplayName("Deve retornar FALHA_ENVIO quando o e-mail do beneficiário for inválido")
    void deveRetornarFalhaQuandoEmailInvalido() {
        @SuppressWarnings("unchecked")
        ObjectProvider<JavaMailSender> provider = mock(ObjectProvider.class);
        GuiaAssinaturaEmailService service = new GuiaAssinaturaEmailService(provider, true, "noreply@fusex.eb.mil.br");

        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setNumeroGuia("202610703");
        guia.setBeneficiarioEmail("email-invalido");

        ResultadoEnvioEmail resultado = service.enviarEmailGuiaAssinada(guia, new byte[]{1}, "Mensagem pronta");

        assertNotNull(resultado);
        assertFalse(resultado.sucesso());
        assertEquals(StatusEmail.FALHA_ENVIO, resultado.status());
        assertFalse(resultado.retentavel());
    }
}

