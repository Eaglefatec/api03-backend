package com.eagle.fusex.service;

import com.eagle.fusex.model.dto.response.GuiaValidadeResponse;
import com.eagle.fusex.model.entity.GuiaAssinatura;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.enums.StatusEsteiraAssinatura;
import com.eagle.fusex.repository.GuiaAssinaturaRepository;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.repository.TriagemPreGuiaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class GuiaValidadeService {

    private final SolicitacaoMedicaRepository solicitacaoRepository;
    private final TriagemPreGuiaRepository triagemRepository;
    private final GuiaAssinaturaRepository guiaAssinaturaRepository;

    public GuiaValidadeService(SolicitacaoMedicaRepository solicitacaoRepository,
                               TriagemPreGuiaRepository triagemRepository) {
        this(solicitacaoRepository, triagemRepository, null);
    }

    @Autowired
    public GuiaValidadeService(SolicitacaoMedicaRepository solicitacaoRepository,
                               TriagemPreGuiaRepository triagemRepository,
                               GuiaAssinaturaRepository guiaAssinaturaRepository) {
        this.solicitacaoRepository = solicitacaoRepository;
        this.triagemRepository = triagemRepository;
        this.guiaAssinaturaRepository = guiaAssinaturaRepository;
    }

    @Transactional(readOnly = true)
    public GuiaValidadeResponse consultarValidade(String numeroGuia) {
        if (guiaAssinaturaRepository != null) {
            Optional<GuiaAssinatura> assinaturaOpt = guiaAssinaturaRepository.findByNumeroGuia(numeroGuia);
            if (assinaturaOpt.isPresent()) {
                GuiaAssinatura ga = assinaturaOpt.get();
                boolean valida = true;
                String motivo = null;

                if (ga.getStatus() == StatusEsteiraAssinatura.REJEITADA) {
                    valida = false;
                    motivo = "GUIA_REJEITADA";
                } else if (ga.getStatus() == StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE
                        || ga.getStatus() == StatusEsteiraAssinatura.AGUARDANDO_CHEFE_SECAO) {
                    valida = false;
                    motivo = "AGUARDANDO_ASSINATURA";
                } else if (ga.getDataValidade() != null && LocalDate.now().isAfter(ga.getDataValidade())) {
                    valida = false;
                    motivo = "GUIA_EXPIRADA";
                }

                return new GuiaValidadeResponse(
                        numeroGuia,
                        valida,
                        motivo,
                        ga.getDataAssinaturaSecao() != null ? ga.getDataAssinaturaSecao() : ga.getDataEnvio(),
                        ga.getDataValidade(),
                        ga.getBeneficiarioNome(),
                        ga.getOcsNome()
                );
            }
        }

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
