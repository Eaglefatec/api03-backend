package com.eagle.fusex.service;

import com.eagle.fusex.model.dto.ResultadoEnvioEmail;
import com.eagle.fusex.model.entity.GuiaAssinatura;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;

@Service
public class GuiaAssinaturaEmailService {

    private static final Logger log = LoggerFactory.getLogger(GuiaAssinaturaEmailService.class);

    private static final java.util.regex.Pattern EMAIL_VALIDO_PATTERN = java.util.regex.Pattern
            .compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final boolean mailEnabled;
    private final String remetente;

    public GuiaAssinaturaEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${fusex.mail.enabled:false}") boolean mailEnabled,
            @Value("${fusex.mail.from:noreply@fusex.eb.mil.br}") String remetente) {
        this.mailSenderProvider = mailSenderProvider;
        this.mailEnabled = mailEnabled;
        this.remetente = remetente;
    }

    public ResultadoEnvioEmail enviarEmailGuiaAssinada(GuiaAssinatura guia, byte[] pdfAssinadoBytes,
            String mensagemPronta) {
        if (guia == null) {
            return ResultadoEnvioEmail.falhaInvalido("Guia de assinatura nula");
        }

        String destinatario = guia.getBeneficiarioEmail() != null ? guia.getBeneficiarioEmail().trim() : null;
        if (destinatario == null || destinatario.isEmpty() || !EMAIL_VALIDO_PATTERN.matcher(destinatario).matches()) {
            log.warn("Guia #{} sem e-mail válido para envio: {}", guia.getNumeroGuia(), destinatario);
            return ResultadoEnvioEmail
                    .falhaInvalido("Endereço de e-mail do beneficiário inválido ou ausente: " + destinatario);
        }

        if (!mailEnabled) {
            log.info("[SIMULAÇÃO DE E-MAIL FUSEX - GUIA ASSINADA] Para: {} | Guia nº: {} | OCS: {} | Válida até: {}",
                    destinatario, guia.getNumeroGuia(), guia.getOcsNome(), guia.getDataValidade());
            return ResultadoEnvioEmail.simulado(destinatario);
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.info("[SIMULAÇÃO DE E-MAIL FUSEX - GUIA ASSINADA] Para: {} | Guia nº: {} | OCS: {} | Válida até: {}",
                    destinatario, guia.getNumeroGuia(), guia.getOcsNome(), guia.getDataValidade());
            return ResultadoEnvioEmail.simulado(destinatario);
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            String numeroLimpoAssunto = guia.getNumeroGuia() != null
                    ? guia.getNumeroGuia().replaceAll("[\\r\\n]+", " ").trim()
                    : "";
            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject("FUSEX - Guia de Encaminhamento nº " + numeroLimpoAssunto + " Assinada");

            String html = """
                    <!DOCTYPE html>
                    <html>
                    <body style="font-family: Arial, sans-serif; color: #17212b; line-height: 1.6; max-width: 600px; margin: 0 auto; padding: 20px;">
                        <div style="background-color: #0b3d1b; color: white; padding: 16px 20px; border-radius: 6px 6px 0 0; text-align: center;">
                            <h2 style="margin: 0; font-size: 20px;">SEÇÃO FUSEX - SISMED</h2>
                            <p style="margin: 4px 0 0 0; font-size: 13px; opacity: 0.9;">Guia de Encaminhamento Emitida e Assinada Digitalmente</p>
                        </div>
                        <div style="border: 1px solid #d6dee8; border-top: none; padding: 24px; border-radius: 0 0 6px 6px; background-color: #ffffff;">
                            <div style="white-space: pre-line; font-size: 15px; background-color: #eef2f7; padding: 16px; border-radius: 8px; border: 1px solid #d6dee8;">%s</div>
                            <p style="font-size: 12px; color: #5b6b7c; margin-top: 20px;">Esta é uma mensagem automática emitida pelo sistema FUSEX. O arquivo PDF assinado encontra-se em anexo.</p>
                        </div>
                    </body>
                    </html>
                    """
                    .formatted(escapeHtml(mensagemPronta));

            helper.setText(mensagemPronta != null ? mensagemPronta : "", html);

            if (pdfAssinadoBytes != null && pdfAssinadoBytes.length > 0) {
                String slugAnexo = numeroLimpoAssunto.replaceAll("[^A-Za-z0-9_-]", "_");
                if (slugAnexo.isBlank())
                    slugAnexo = "documento";
                String nomeAnexo = "guia_" + slugAnexo + ".pdf";
                helper.addAttachment(nomeAnexo, new ByteArrayResource(pdfAssinadoBytes), "application/pdf");
            }

            mailSender.send(mimeMessage);
            log.info("E-mail da guia assinada #{} enviado com sucesso para: {}", guia.getNumeroGuia(), destinatario);
            return ResultadoEnvioEmail.sucesso(destinatario);
        } catch (MailException e) {
            log.warn("Falha de comunicação SMTP ao enviar guia #{} para {}: {}", guia.getNumeroGuia(), destinatario,
                    e.getMessage());
            return ResultadoEnvioEmail.falhaConexao("Falha de comunicação com servidor SMTP: " + e.getMessage());
        } catch (MessagingException e) {
            log.error("Falha de montagem MIME ao enviar guia #{} para {}: {}", guia.getNumeroGuia(), destinatario,
                    e.getMessage());
            return ResultadoEnvioEmail.falhaConexao("Erro no envio SMTP: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao enviar e-mail da guia #{} para {}: {}", guia.getNumeroGuia(), destinatario,
                    e.getMessage(), e);
            return ResultadoEnvioEmail.falhaInvalido("Erro inesperado: " + e.getMessage());
        }
    }

    private String escapeHtml(String input) {
        if (input == null) {
            return "";
        }
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
