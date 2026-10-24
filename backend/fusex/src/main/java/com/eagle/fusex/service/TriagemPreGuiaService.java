package com.eagle.fusex.service;

import com.eagle.fusex.exception.OcsInvalidoException;
import com.eagle.fusex.exception.SolicitacaoNaoEncontradaException;
import com.eagle.fusex.exception.TriagemJaPreenchidaException;
import com.eagle.fusex.model.dto.request.PreencherTriagemRequest;
import com.eagle.fusex.model.dto.response.SolicitacaoPublicaResponse;
import com.eagle.fusex.model.dto.response.TriagemResponse;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.OcsProcedimento;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.entity.SolicitacaoProcedimento;
import com.eagle.fusex.model.entity.TriagemPreGuia;
import com.eagle.fusex.model.enums.Especialidade;
import com.eagle.fusex.repository.OcsProcedimentoRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.repository.PacienteRepository;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.repository.SolicitacaoProcedimentoRepository;
import com.eagle.fusex.repository.TriagemPreGuiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class TriagemPreGuiaService {

    private final SolicitacaoMedicaRepository solicitacaoRepository;
    private final TriagemPreGuiaRepository triagemRepository;
    private final OcsRepository ocsRepository;
    private final SolicitacaoProcedimentoRepository solicitacaoProcedimentoRepository;
    private final OcsProcedimentoRepository ocsProcedimentoRepository;
    private final PacienteRepository pacienteRepository;

    public TriagemPreGuiaService(SolicitacaoMedicaRepository solicitacaoRepository,
                                 TriagemPreGuiaRepository triagemRepository,
                                 OcsRepository ocsRepository,
                                 SolicitacaoProcedimentoRepository solicitacaoProcedimentoRepository,
                                 OcsProcedimentoRepository ocsProcedimentoRepository,
                                 PacienteRepository pacienteRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.triagemRepository = triagemRepository;
        this.ocsRepository = ocsRepository;
        this.solicitacaoProcedimentoRepository = solicitacaoProcedimentoRepository;
        this.ocsProcedimentoRepository = ocsProcedimentoRepository;
        this.pacienteRepository = pacienteRepository;
    }

    @Transactional(readOnly = true)
    public SolicitacaoPublicaResponse buscarPorToken(String token) {
        SolicitacaoMedica solicitacao = validarSolicitacao(token);

        boolean triagemPreenchida = triagemRepository.findBySolicitacaoMedicaId(solicitacao.getId())
                .isPresent();

        List<SolicitacaoPublicaResponse.ProcedimentoResumo> procedimentos = solicitacaoProcedimentoRepository
                .findBySolicitacaoMedicaId(solicitacao.getId()).stream()
                .map(sp -> new SolicitacaoPublicaResponse.ProcedimentoResumo(
                        sp.getProcedimento().getProcCodigoDgp(),
                        sp.getProcedimento().getProcDescricao()
                ))
                .toList();

        Set<Especialidade> especialidadesCopia =
                solicitacao.getEspecialidades() != null
                        ? new HashSet<>(solicitacao.getEspecialidades())
                        : Set.of();

        Paciente paciente = solicitacao.getPaciente();

        return new SolicitacaoPublicaResponse(
                paciente != null ? paciente.getNome() : null,
                paciente != null ? paciente.getOm() : null,
                paciente != null ? paciente.getCpfPrec() : null,
                paciente != null ? paciente.getIdade() : null,
                paciente != null ? paciente.getTelefone() : null,
                especialidadesCopia,
                solicitacao.getObservacao(),
                triagemPreenchida,
                procedimentos
        );
    }

    @Transactional
    public TriagemResponse preencherTriagem(String token, PreencherTriagemRequest request) {
        SolicitacaoMedica solicitacao = validarSolicitacao(token);

        if (triagemRepository.findBySolicitacaoMedicaId(solicitacao.getId()).isPresent()) {
            throw new TriagemJaPreenchidaException("Triagem já foi preenchida para esta solicitação");
        }

        Ocs ocs = ocsRepository.findById(request.getOcsId())
                .orElseThrow(() -> new OcsInvalidoException("Selecione uma clínica/OCS válida"));

        TriagemPreGuia triagem = new TriagemPreGuia();
        triagem.setSolicitacaoMedica(solicitacao);
        triagem.setCpfPrec(request.getCpfPrec());
        triagem.setIdade(request.getIdade());
        triagem.setTelefone(request.getTelefone());
        triagem.setOcs(ocs);
        triagem.setAceitoTermos(request.getAceitoTermos());

        triagemRepository.save(triagem);

        // Atualizar dados cadastrais faltantes no Paciente
        Paciente paciente = solicitacao.getPaciente();
        if (paciente != null) {
            if (request.getIdade() != null) {
                paciente.setIdade(request.getIdade());
            }
            if (request.getTelefone() != null && !request.getTelefone().isBlank()) {
                paciente.setTelefone(request.getTelefone().trim());
            }
            if (request.getCpfPrec() != null && !request.getCpfPrec().isBlank()) {
                paciente.setCpfPrec(request.getCpfPrec().trim());
            }
            pacienteRepository.save(paciente);
        }

        snapshotValoresProcedimentos(solicitacao, ocs);

        List<TriagemResponse.Opcao> opcoes = Arrays.asList(
                new TriagemResponse.Opcao("PRESENCIAL", "Agendamento presencial"),
                new TriagemResponse.Opcao("PRE_GUIA_DIGITAL", "Pré-guia digital")
        );

        return new TriagemResponse("Triagem preenchida com sucesso", opcoes);
    }

    private void snapshotValoresProcedimentos(SolicitacaoMedica solicitacao, Ocs ocs) {
        List<SolicitacaoProcedimento> vinculos = solicitacaoProcedimentoRepository
                .findBySolicitacaoMedicaId(solicitacao.getId());

        if (vinculos.isEmpty()) {
            return;
        }

        List<String> codigos = vinculos.stream()
                .map(v -> v.getProcedimento().getProcCodigoDgp())
                .toList();

        Map<String, BigDecimal> precosPorCodigo = ocsProcedimentoRepository
                .findByOcs_OcsIdAndProcedimento_ProcCodigoDgpIn(ocs.getOcsId(), codigos).stream()
                .collect(Collectors.toMap(
                        op -> op.getProcedimento().getProcCodigoDgp(),
                        OcsProcedimento::getValor,
                        (v1, v2) -> v1
                ));

        for (SolicitacaoProcedimento vinculo : vinculos) {
            BigDecimal valor = precosPorCodigo.get(vinculo.getProcedimento().getProcCodigoDgp());
            if (valor != null) {
                vinculo.setValorNoMomento(valor);
            }
        }

        solicitacaoProcedimentoRepository.saveAll(vinculos);
    }

    private SolicitacaoMedica validarSolicitacao(String token) {
        return solicitacaoRepository.findByTokenPublico(token)
                .filter(SolicitacaoMedica::getValida)
                .orElseThrow(() -> new SolicitacaoNaoEncontradaException(
                        "Solicitação não encontrada ou inválida"));
    }
}
