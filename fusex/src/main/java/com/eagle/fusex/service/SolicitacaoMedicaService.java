package com.eagle.fusex.service;

import com.eagle.fusex.exception.MedicoInvalidoException;
import com.eagle.fusex.exception.ProcedimentoInvalidoException;
import com.eagle.fusex.model.dto.ResultadoEnvioEmail;
import com.eagle.fusex.model.dto.request.CriarSolicitacaoRequest;
import com.eagle.fusex.model.dto.response.SolicitacaoResponse;
import com.eagle.fusex.model.entity.Medico;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.entity.SolicitacaoProcedimento;
import com.eagle.fusex.model.enums.StatusEmail;
import com.eagle.fusex.repository.MedicoRepository;
import com.eagle.fusex.repository.PacienteRepository;
import com.eagle.fusex.repository.ProcedimentoRepository;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.repository.SolicitacaoProcedimentoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class SolicitacaoMedicaService {

    private static final Logger log = LoggerFactory.getLogger(SolicitacaoMedicaService.class);

    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final SolicitacaoMedicaRepository solicitacaoRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final SolicitacaoProcedimentoRepository solicitacaoProcedimentoRepository;
    private final SolicitacaoEmailService emailService;

    @Autowired
    public SolicitacaoMedicaService(MedicoRepository medicoRepository,
                                  PacienteRepository pacienteRepository,
                                  SolicitacaoMedicaRepository solicitacaoRepository,
                                  ProcedimentoRepository procedimentoRepository,
                                  SolicitacaoProcedimentoRepository solicitacaoProcedimentoRepository,
                                  @Autowired(required = false) SolicitacaoEmailService emailService) {
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.solicitacaoProcedimentoRepository = solicitacaoProcedimentoRepository;
        this.emailService = emailService;
    }

    public SolicitacaoMedicaService(MedicoRepository medicoRepository,
                                  PacienteRepository pacienteRepository,
                                  SolicitacaoMedicaRepository solicitacaoRepository,
                                  ProcedimentoRepository procedimentoRepository,
                                  SolicitacaoProcedimentoRepository solicitacaoProcedimentoRepository) {
        this(medicoRepository, pacienteRepository, solicitacaoRepository, procedimentoRepository, solicitacaoProcedimentoRepository, null);
    }

    @Transactional
    public SolicitacaoResponse criar(CriarSolicitacaoRequest request) {
        Medico medico = validarMedico(request.getMedicoId());

        String cpfNormalizado = Paciente.normalizarCpf(request.getCpf());
        String precTrim = request.getPrec() != null ? request.getPrec().trim() : "";

        Paciente paciente = pacienteRepository.findByCpfOrPrec(cpfNormalizado, precTrim)
                .orElseGet(() -> criarNovoPaciente(request, cpfNormalizado, precTrim));

        atualizarDadosPaciente(paciente, request, cpfNormalizado, precTrim);

        invalidarSolicitacaoAnterior(medico.getId(), paciente.getId());

        SolicitacaoMedica solicitacao = new SolicitacaoMedica();
        solicitacao.setMedico(medico);
        if (request.getMedicoResponsavelId() != null) {
            Medico medicoResponsavel = medicoRepository.findById(request.getMedicoResponsavelId())
                    .orElseThrow(() -> new MedicoInvalidoException("Selecione um médico responsável válido"));
            solicitacao.setMedicoResponsavel(medicoResponsavel);
        }
        solicitacao.setPaciente(paciente);
        solicitacao.setEspecialidades(request.getEspecialidades());
        solicitacao.setObservacao(request.getObservacao());
        solicitacao.setTokenPublico(UUID.randomUUID().toString());
        solicitacao.setValida(true);
        solicitacao.setEmailDestinatario(paciente.getEmail());

        SolicitacaoMedica salva = solicitacaoRepository.save(solicitacao);

        persistirProcedimentos(salva, request.getProcedimentos());

        ResultadoEnvioEmail resultadoEmail = null;
        if (emailService != null) {
            try {
                resultadoEmail = emailService.enviarEmailSolicitacao(salva);
            } catch (Exception e) {
                log.error("Erro inesperado ao disparar e-mail da solicitação #{}", salva.getId(), e);
                resultadoEmail = ResultadoEnvioEmail.falhaConexao("Erro interno no disparo: " + e.getMessage());
            }
        }

        if (resultadoEmail != null) {
            salva.setStatusEmail(resultadoEmail.status());
            salva.setEmailUltimoErro(resultadoEmail.mensagem());
            if (resultadoEmail.status() == StatusEmail.PENDENTE_REENVIO) {
                salva.setEmailTentativas(1);
                salva.setEmailProximaTentativa(LocalDateTime.now().plusMinutes(2));
            } else if (resultadoEmail.status() == StatusEmail.ENVIADO) {
                salva.setEmailTentativas(1);
                salva.setEmailProximaTentativa(null);
            }
            solicitacaoRepository.save(salva);
        }

        boolean emailEnviado = resultadoEmail != null && resultadoEmail.sucesso();
        StatusEmail statusFinal = resultadoEmail != null ? resultadoEmail.status() : StatusEmail.SIMULADO;
        String emailMsg = resultadoEmail != null ? resultadoEmail.mensagem() : "Disparo de e-mail não executado";

        String linkBeneficiario = "/solicitacoes/publico/" + salva.getTokenPublico();
        String qrCodeUrl = "/solicitacoes/publico/" + salva.getTokenPublico() + "/qrcode";

        return new SolicitacaoResponse(
                salva.getId(),
                salva.getTokenPublico(),
                linkBeneficiario,
                "Solicitação médica criada com sucesso",
                emailEnviado,
                qrCodeUrl,
                statusFinal,
                salva.getEmailDestinatario(),
                emailMsg
        );
    }

    @Transactional
    public SolicitacaoResponse reenviarEmail(Long solicitacaoId, String novoEmail) {
        SolicitacaoMedica solicitacao = solicitacaoRepository.findById(solicitacaoId)
                .orElseThrow(() -> new IllegalArgumentException("Solicitação não encontrada: " + solicitacaoId));

        if (novoEmail != null && !novoEmail.isBlank()) {
            String emailLimpo = novoEmail.trim();
            if (solicitacao.getPaciente() != null) {
                solicitacao.getPaciente().setEmail(emailLimpo);
                pacienteRepository.save(solicitacao.getPaciente());
            }
            solicitacao.setEmailDestinatario(emailLimpo);
        }

        ResultadoEnvioEmail resultado = null;
        if (emailService != null) {
            try {
                resultado = emailService.enviarEmailSolicitacao(solicitacao);
            } catch (Exception e) {
                log.error("Erro inesperado ao reenviar e-mail da solicitação #{}", solicitacao.getId(), e);
                resultado = ResultadoEnvioEmail.falhaConexao("Erro interno ao reenviar: " + e.getMessage());
            }
        }

        if (resultado != null) {
            solicitacao.setStatusEmail(resultado.status());
            solicitacao.setEmailUltimoErro(resultado.mensagem());
            solicitacao.setEmailTentativas(solicitacao.getEmailTentativas() + 1);
            if (resultado.status() == StatusEmail.PENDENTE_REENVIO) {
                solicitacao.setEmailProximaTentativa(LocalDateTime.now().plusMinutes(2));
            } else if (resultado.status() == StatusEmail.ENVIADO) {
                solicitacao.setEmailProximaTentativa(null);
            }
            solicitacaoRepository.save(solicitacao);
        }

        boolean emailEnviado = resultado != null && resultado.sucesso();
        StatusEmail statusFinal = resultado != null ? resultado.status() : StatusEmail.SIMULADO;
        String emailMsg = resultado != null ? resultado.mensagem() : "Reenvio não executado";

        String linkBeneficiario = "/solicitacoes/publico/" + solicitacao.getTokenPublico();
        String qrCodeUrl = "/solicitacoes/publico/" + solicitacao.getTokenPublico() + "/qrcode";

        return new SolicitacaoResponse(
                solicitacao.getId(),
                solicitacao.getTokenPublico(),
                linkBeneficiario,
                resultado != null && resultado.sucesso() ? "E-mail reenviado com sucesso" : "Tentativa de reenvio processada",
                emailEnviado,
                qrCodeUrl,
                statusFinal,
                solicitacao.getEmailDestinatario(),
                emailMsg
        );
    }

    private Medico validarMedico(Long medicoId) {
        return medicoRepository.findByIdAndCredenciadoTrue(medicoId)
                .orElseThrow(() -> new MedicoInvalidoException("Selecione um médico válido"));
    }

    private Paciente criarNovoPaciente(CriarSolicitacaoRequest request, String cpf, String prec) {
        String email = request.getEmail() != null ? request.getEmail().trim() : "";
        Paciente novoPaciente = new Paciente(request.getNomePaciente(), cpf, prec, request.getOm(), email);
        return pacienteRepository.save(novoPaciente);
    }

    private void atualizarDadosPaciente(Paciente paciente, CriarSolicitacaoRequest request, String cpf, String prec) {
        if (paciente.getNome() == null || paciente.getNome().isBlank()) {
            paciente.setNome(request.getNomePaciente());
        }
        if (paciente.getOm() == null || paciente.getOm().isBlank()) {
            paciente.setOm(request.getOm());
        }
        if (cpf != null && !cpf.isBlank()) {
            paciente.setCpf(cpf);
        }
        if (prec != null && !prec.isBlank()) {
            paciente.setPrec(prec);
        }
        if (request.getEmail() != null && !request.getEmail().isBlank()) {
            paciente.setEmail(request.getEmail().trim());
        }
        pacienteRepository.save(paciente);
    }

    private void persistirProcedimentos(SolicitacaoMedica solicitacao,
                                        List<CriarSolicitacaoRequest.ProcedimentoQuantidade> procedimentosRequest) {
        List<SolicitacaoProcedimento> vinculos = procedimentosRequest.stream()
                .map(pq -> {
                    Procedimento procedimento = procedimentoRepository.findById(pq.getProcedimentoCodigoDgp())
                            .orElseThrow(() -> new ProcedimentoInvalidoException(
                                    "Procedimento inválido: " + pq.getProcedimentoCodigoDgp()));
                    SolicitacaoProcedimento sp = new SolicitacaoProcedimento();
                    sp.setSolicitacaoMedica(solicitacao);
                    sp.setProcedimento(procedimento);
                    sp.setQuantidade(pq.getQuantidade());
                    return sp;
                })
                .toList();
        solicitacaoProcedimentoRepository.saveAll(vinculos);
    }

    private void invalidarSolicitacaoAnterior(Long medicoId, Long pacienteId) {
        List<SolicitacaoMedica> solicitacoesAtivas =
                solicitacaoRepository.findByMedicoIdAndPacienteIdAndValidaTrue(medicoId, pacienteId);

        for (SolicitacaoMedica anterior : solicitacoesAtivas) {
            anterior.setValida(false);
            solicitacaoRepository.save(anterior);
        }
    }
}
