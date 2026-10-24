package com.eagle.fusex.service;

import com.eagle.fusex.model.dto.response.GuiaValidadeResponse;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.repository.TriagemPreGuiaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class GuiaValidadeService {

    private final SolicitacaoMedicaRepository solicitacaoRepository;
    private final TriagemPreGuiaRepository triagemRepository;

    public GuiaValidadeService(SolicitacaoMedicaRepository solicitacaoRepository,
                               TriagemPreGuiaRepository triagemRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.triagemRepository = triagemRepository;
    }

    @Transactional(readOnly = true)
    public GuiaValidadeResponse consultarValidade(String numeroGuia) {
        Optional<SolicitacaoMedica> solicitacaoOpt = solicitacaoRepository.findByNumeroGuia(numeroGuia);
        if (solicitacaoOpt.isEmpty()) {
            return new GuiaValidadeResponse(numeroGuia, false, "NUMERO_NAO_ENCONTRADO", null, null, null, null);
        }

        SolicitacaoMedica solicitacao = solicitacaoOpt.get();
        boolean valida = true;
        String motivo = null;

        if (!Boolean.TRUE.equals(solicitacao.getValida())) {
            valida = false;
            motivo = "GUIA_SUBSTITUIDA";
        } else if (solicitacao.getDataValidade() != null && LocalDate.now().isAfter(solicitacao.getDataValidade())) {
            valida = false;
            motivo = "GUIA_EXPIRADA";
        }

        String ocsVinculada = triagemRepository.findBySolicitacaoMedicaId(solicitacao.getId())
                .map(triagem -> triagem.getOcs().getOcsNome())
                .orElse(null);

        String nomePaciente = solicitacao.getPaciente() != null ? solicitacao.getPaciente().getNome() : null;

        return new GuiaValidadeResponse(numeroGuia, valida, motivo, solicitacao.getDataImportacaoGuia(),
                solicitacao.getDataValidade(), nomePaciente, ocsVinculada);
    }
}
