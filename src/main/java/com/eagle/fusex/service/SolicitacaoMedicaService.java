package com.eagle.fusex.service;

import com.eagle.fusex.exception.MedicoInvalidoException;
import com.eagle.fusex.exception.ProcedimentoInvalidoException;
import com.eagle.fusex.model.dto.request.CriarSolicitacaoRequest;
import com.eagle.fusex.model.dto.response.SolicitacaoResponse;
import com.eagle.fusex.model.entity.Medico;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.entity.SolicitacaoProcedimento;
import com.eagle.fusex.repository.MedicoRepository;
import com.eagle.fusex.repository.PacienteRepository;
import com.eagle.fusex.repository.ProcedimentoRepository;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.repository.SolicitacaoProcedimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SolicitacaoMedicaService {

    private final MedicoRepository medicoRepository;
    private final PacienteRepository pacienteRepository;
    private final SolicitacaoMedicaRepository solicitacaoRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final SolicitacaoProcedimentoRepository solicitacaoProcedimentoRepository;

    public SolicitacaoMedicaService(MedicoRepository medicoRepository,
                                    PacienteRepository pacienteRepository,
                                    SolicitacaoMedicaRepository solicitacaoRepository,
                                    ProcedimentoRepository procedimentoRepository,
                                    SolicitacaoProcedimentoRepository solicitacaoProcedimentoRepository) {
        this.medicoRepository = medicoRepository;
        this.pacienteRepository = pacienteRepository;
        this.solicitacaoRepository = solicitacaoRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.solicitacaoProcedimentoRepository = solicitacaoProcedimentoRepository;
    }

    @Transactional
    public SolicitacaoResponse criar(CriarSolicitacaoRequest request) {
        Medico medico = validarMedico(request.getMedicoId());

        Paciente paciente = pacienteRepository.findByCpfPrec(request.getCpfPrec())
                .orElseGet(() -> criarNovoPaciente(request));

        atualizarDadosPaciente(paciente, request);

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

        SolicitacaoMedica salva = solicitacaoRepository.save(solicitacao);

        persistirProcedimentos(salva, request.getProcedimentos());

        String linkBeneficiario = "/solicitacoes/publico/" + salva.getTokenPublico();

        return new SolicitacaoResponse(
                salva.getId(),
                salva.getTokenPublico(),
                linkBeneficiario,
                "Solicitação médica criada com sucesso"
        );
    }

    private Medico validarMedico(Long medicoId) {
        return medicoRepository.findByIdAndCredenciadoTrue(medicoId)
                .orElseThrow(() -> new MedicoInvalidoException("Selecione um médico válido"));
    }

    private Paciente criarNovoPaciente(CriarSolicitacaoRequest request) {
        Paciente novoPaciente = new Paciente(request.getNomePaciente(), request.getCpfPrec(), request.getOm());
        return pacienteRepository.save(novoPaciente);
    }

    private void atualizarDadosPaciente(Paciente paciente, CriarSolicitacaoRequest request) {
        if (paciente.getNome() == null || paciente.getNome().isBlank()) {
            paciente.setNome(request.getNomePaciente());
        }
        if (paciente.getOm() == null || paciente.getOm().isBlank()) {
            paciente.setOm(request.getOm());
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
                    SolicitacaoProcedimento vinculo = new SolicitacaoProcedimento();
                    vinculo.setSolicitacaoMedica(solicitacao);
                    vinculo.setProcedimento(procedimento);
                    vinculo.setQuantidade(pq.getQuantidade());
                    return vinculo;
                }).toList();

        solicitacaoProcedimentoRepository.saveAll(vinculos);
    }

    private void invalidarSolicitacaoAnterior(Long medicoId, Long pacienteId) {
        List<SolicitacaoMedica> anteriores = solicitacaoRepository
                .findByMedicoIdAndPacienteIdAndValidaTrue(medicoId, pacienteId);

        for (SolicitacaoMedica sol : anteriores) {
            sol.setValida(false);
            solicitacaoRepository.save(sol);
        }
    }
}
