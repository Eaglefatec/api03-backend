package com.eagle.fusex.scheduler;

import com.eagle.fusex.model.dto.ResultadoEnvioEmail;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.enums.StatusEmail;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.service.SolicitacaoEmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class EmailRetryScheduler {

    private static final Logger log = LoggerFactory.getLogger(EmailRetryScheduler.class);
    public static final int MAX_TENTATIVAS = 5;

    private final SolicitacaoMedicaRepository solicitacaoRepository;
    private final SolicitacaoEmailService emailService;

    public EmailRetryScheduler(SolicitacaoMedicaRepository solicitacaoRepository,
                               SolicitacaoEmailService emailService) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.emailService = emailService;
    }

    @Scheduled(fixedDelay = 60000, initialDelay = 10000)
    @Transactional
    public void processarFilaReenvioEmail() {
        if (emailService == null) {
            return;
        }

        LocalDateTime agora = LocalDateTime.now();
        List<SolicitacaoMedica> pendentes = solicitacaoRepository
                .findByStatusEmailAndEmailProximaTentativaLessThanEqualAndEmailTentativasLessThan(
                        StatusEmail.PENDENTE_REENVIO, agora, MAX_TENTATIVAS);

        if (pendentes.isEmpty()) {
            return;
        }

        log.info("Processando fila de reenvio de e-mails: {} solicitação(ões) pendente(s)", pendentes.size());

        for (SolicitacaoMedica solicitacao : pendentes) {
            try {
                int tentativas = (solicitacao.getEmailTentativas() != null ? solicitacao.getEmailTentativas() : 0) + 1;
                solicitacao.setEmailTentativas(tentativas);

                ResultadoEnvioEmail resultado = emailService.enviarEmailSolicitacao(solicitacao);

                if (resultado.sucesso()) {
                    solicitacao.setStatusEmail(StatusEmail.ENVIADO);
                    solicitacao.setEmailUltimoErro(resultado.mensagem());
                    solicitacao.setEmailProximaTentativa(null);
                    log.info("E-mail da solicitação #{} reenviado com sucesso na tentativa {}", solicitacao.getId(), tentativas);
                } else if (resultado.status() == StatusEmail.PENDENTE_REENVIO) {
                    if (tentativas >= MAX_TENTATIVAS) {
                        solicitacao.setStatusEmail(StatusEmail.FALHA_ENVIO);
                        solicitacao.setEmailUltimoErro("Limite de " + MAX_TENTATIVAS + " tentativas excedido: " + resultado.mensagem());
                        solicitacao.setEmailProximaTentativa(null);
                        log.warn("Solicitação #{} atingiu limite de retentativas de e-mail e foi marcada como FALHA_ENVIO", solicitacao.getId());
                    } else {
                        long minutosEspera = (long) tentativas * 2;
                        solicitacao.setEmailProximaTentativa(LocalDateTime.now().plusMinutes(minutosEspera));
                        solicitacao.setEmailUltimoErro(resultado.mensagem());
                        log.info("Tentativa {} falhou para solicitação #{}. Próxima retentativa em {} min", solicitacao.getId(), tentativas, minutosEspera);
                    }
                } else {
                    solicitacao.setStatusEmail(resultado.status());
                    solicitacao.setEmailUltimoErro(resultado.mensagem());
                    solicitacao.setEmailProximaTentativa(null);
                }

                solicitacaoRepository.save(solicitacao);
            } catch (Exception e) {
                log.error("Erro inesperado ao reprocessar e-mail da solicitação #{}", solicitacao.getId(), e);
            }
        }
    }
}

