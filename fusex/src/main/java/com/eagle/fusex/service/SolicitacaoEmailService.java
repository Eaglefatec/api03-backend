package com.eagle.fusex.service;

import com.eagle.fusex.model.dto.ResultadoEnvioEmail;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
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
public class SolicitacaoEmailService {

    private static final Logger log = LoggerFactory.getLogger(SolicitacaoEmailService.class);

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final QrCodeService qrCodeService;
    private final boolean mailEnabled;
    private final String remetente;

    public SolicitacaoEmailService(ObjectProvider<JavaMailSender> mailSenderProvider,
                                   QrCodeService qrCodeService,
                                   @Value("${fusex.mail.enabled:false}") boolean mailEnabled,
                                   @Value("${fusex.mail.from:noreply@fusex.eb.mil.br}") String remetente) {
        this.mailSenderProvider = mailSenderProvider;
        this.qrCodeService = qrCodeService;
        this.mailEnabled = mailEnabled;
        this.remetente = remetente;
    }

    public ResultadoEnvioEmail enviarEmailSolicitacao(SolicitacaoMedica solicitacao) {
        if (solicitacao == null || solicitacao.getPaciente() == null) {
            return ResultadoEnvioEmail.falhaInvalido("Solicitação ou paciente nulos");
        }

        String destinatario = solicitacao.getPaciente().getEmail();
        if (destinatario == null || destinatario.isBlank() || !destinatario.contains("@")) {
            log.warn("Solicitação #{} sem e-mail válido para envio: {}", solicitacao.getId(), destinatario);
            return ResultadoEnvioEmail.falhaInvalido("Endereço de e-mail do paciente inválido ou ausente: " + destinatario);
        }

        String link = qrCodeService.obterLinkPublicoBeneficiario(solicitacao.getTokenPublico());
        byte[] qrCodeBytes = qrCodeService.gerarQrCodePngPorToken(solicitacao.getTokenPublico());

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (!mailEnabled || mailSender == null) {
            log.info("[SIMULAÇÃO DE E-MAIL FUSEX] E-mail para: {} | Link: {} | Token: {}",
                    destinatario, link, solicitacao.getTokenPublico());
            return ResultadoEnvioEmail.simulado(destinatario);
        }

        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, StandardCharsets.UTF_8.name());

            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject("FUSEX - Sua Solicitação Médica está Disponível");

            String nomePaciente = solicitacao.getPaciente().getNome();
            String nomeMedico = solicitacao.getMedico() != null ? solicitacao.getMedico().getNome() : "Médico Militar";

            String html = """
                <!DOCTYPE html>
                <html>
                <body style="font-family: Arial, sans-serif; color: #333; line-height: 1.6; max-width: 600px; margin: 0 auto; padding: 20px;">
                    <div style="background-color: #0b3d1b; color: white; padding: 16px 20px; border-radius: 6px 6px 0 0; text-align: center;">
                        <h2 style="margin: 0; font-size: 20px;">FUNDO DE SAÚDE DO EXÉRCITO - FUSEX</h2>
                        <p style="margin: 4px 0 0 0; font-size: 13px; opacity: 0.9;">Sistema Integrado de Acesso Médico e Exames</p>
                    </div>
                    <div style="border: 1px solid #ddd; border-top: none; padding: 24px; border-radius: 0 0 6px 6px; background-color: #fafafa;">
                        <p style="font-size: 16px;">Prezado(a) <strong>%s</strong>,</p>
                        <p>Sua solicitação médica foi gerada com sucesso pelo Dr.(a) <strong>%s</strong>.</p>
                        
                        <div style="background-color: #eef6f0; border-left: 4px solid #0b3d1b; padding: 14px; margin: 20px 0; border-radius: 4px;">
                            <h3 style="margin: 0 0 8px 0; color: #0b3d1b; font-size: 15px;">Acesso Rápido pelo Computador:</h3>
                            <p style="margin: 0 0 12px 0; font-size: 14px;">Clique no botão abaixo para preencher sua triagem e escolher sua clínica credenciada:</p>
                            <div style="text-align: center; margin: 16px 0;">
                                <a href="%s" style="background-color: #0b3d1b; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 4px; font-weight: bold; display: inline-block;">
                                    Acessar Minha Solicitação Médica
                                </a>
                            </div>
                            <p style="font-size: 12px; color: #666; margin: 8px 0 0 0;">Ou utilize o link direto: <a href="%s" style="color: #0b3d1b;">%s</a></p>
                        </div>

                        <div style="background-color: #ffffff; border: 1px solid #e0e0e0; padding: 16px; margin: 20px 0; border-radius: 4px; text-align: center;">
                            <h3 style="margin: 0 0 6px 0; color: #333; font-size: 15px;">Acesso pelo Celular ou Tablet:</h3>
                            <p style="margin: 0 0 12px 0; font-size: 13px; color: #666;">Aponte a câmera do seu smartphone para o QR Code abaixo:</p>
                            <img src="cid:qrcode" alt="QR Code de Acesso" style="width: 200px; height: 200px; border: 1px solid #ccc; border-radius: 4px;" />
                        </div>

                        <p style="font-size: 13px; color: #888; margin-top: 24px;">Esta é uma mensagem automática emitida pelo sistema FUSEX. Por favor, não responda a este e-mail.</p>
                    </div>
                </body>
                </html>
                """.formatted(nomePaciente, nomeMedico, link, link, link);

            helper.setText(html, true);
            helper.addInline("qrcode", new ByteArrayResource(qrCodeBytes), "image/png");

            mailSender.send(mimeMessage);
            log.info("E-mail de solicitação médica enviado com sucesso para: {}", destinatario);
            return ResultadoEnvioEmail.sucesso(destinatario);
        } catch (MailException e) {
            log.warn("Falha de comunicação/rede com o servidor SMTP ao enviar para {}: {}", destinatario, e.getMessage());
            return ResultadoEnvioEmail.falhaConexao("Falha de comunicação com servidor SMTP: " + e.getMessage());
        } catch (MessagingException e) {
            log.error("Falha no protocolo de montagem/envio MIME para {}: {}", destinatario, e.getMessage());
            return ResultadoEnvioEmail.falhaConexao("Erro no envio SMTP: " + e.getMessage());
        } catch (Exception e) {
            log.error("Erro inesperado ao enviar e-mail para {}: {}", destinatario, e.getMessage(), e);
            return ResultadoEnvioEmail.falhaInvalido("Erro inesperado: " + e.getMessage());
        }
    }
}
