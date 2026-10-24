package com.eagle.fusex.service;

import com.eagle.fusex.exception.FechamentoJaEnviadoException;
import com.eagle.fusex.exception.OcsInvalidoException;
import com.eagle.fusex.exception.ValidationException;
import com.eagle.fusex.model.dto.response.FechamentoResponse;
import com.eagle.fusex.model.entity.Fechamento;
import com.eagle.fusex.model.entity.Lote;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.enums.StatusFechamento;
import com.eagle.fusex.repository.FechamentoRepository;
import com.eagle.fusex.repository.GuiaRepository;
import com.eagle.fusex.repository.LoteRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class FechamentoService {

    private static final Logger log = LoggerFactory.getLogger(FechamentoService.class);

    private final FechamentoRepository fechamentoRepository;
    private final LoteRepository loteRepository;
    private final GuiaRepository guiaRepository;
    private final OcsRepository ocsRepository;

    public FechamentoService(FechamentoRepository fechamentoRepository, LoteRepository loteRepository,
                              GuiaRepository guiaRepository, OcsRepository ocsRepository) {
        this.fechamentoRepository = fechamentoRepository;
        this.loteRepository = loteRepository;
        this.guiaRepository = guiaRepository;
        this.ocsRepository = ocsRepository;
    }

    @Transactional
    public FechamentoResponse enviarFechamento(Integer competenciaAno, Integer competenciaMes) {
        Ocs ocs = buscarOcsLogada();

        if (fechamentoRepository.existsByOcs_OcsIdAndCompetenciaAnoAndCompetenciaMes(
                ocs.getOcsId(), competenciaAno, competenciaMes)) {
            throw new FechamentoJaEnviadoException("O fechamento desta competência já foi enviado");
        }

        List<Lote> lotes = loteRepository.findByOcs_OcsIdAndCompetenciaAnoAndCompetenciaMesAndFechamentoIsNull(
                ocs.getOcsId(), competenciaAno, competenciaMes);
        if (lotes.isEmpty()) {
            throw new ValidationException("Não há lotes gerados para esta competência");
        }

        Fechamento fechamento = new Fechamento();
        fechamento.setOcs(ocs);
        fechamento.setCompetenciaAno(competenciaAno);
        fechamento.setCompetenciaMes(competenciaMes);
        fechamento.setDataEnvio(LocalDateTime.now());
        fechamento.setStatus(StatusFechamento.AGUARDANDO_FATURAMENTO);

        Fechamento salvo = fechamentoRepository.save(fechamento);

        lotes.forEach(lote -> lote.setFechamento(salvo));
        loteRepository.saveAll(lotes);

        log.info("Fechamento {}/{} enviado pela OCS {} com {} lotes", competenciaMes, competenciaAno,
                ocs.getOcsNome(), lotes.size());

        return toResponse(salvo, lotes);
    }

    @Transactional(readOnly = true)
    public List<FechamentoResponse> listarFechamentos() {
        Ocs ocs = buscarOcsLogada();
        return fechamentoRepository.findByOcs_LoginUsuarioOrderByDataEnvioDesc(ocs.getLoginUsuario()).stream()
                .map(fechamento -> toResponse(fechamento, loteRepository.findByFechamentoId(fechamento.getId())))
                .toList();
    }

    private Ocs buscarOcsLogada() {
        String loginUsuario = SecurityUtils.getAuthenticatedUsername()
                .orElseThrow(() -> new OcsInvalidoException("Usuário não autenticado"));
        return ocsRepository.findByLoginUsuario(loginUsuario)
                .orElseThrow(() -> new OcsInvalidoException("Nenhuma OCS associada a este usuário"));
    }

    private FechamentoResponse toResponse(Fechamento fechamento, List<Lote> lotes) {
        long quantidadeGuias = lotes.stream()
                .mapToLong(lote -> guiaRepository.countByLoteId(lote.getId()))
                .sum();
        return new FechamentoResponse(fechamento.getId(), fechamento.getCompetenciaAno(),
                fechamento.getCompetenciaMes(), fechamento.getDataEnvio(), fechamento.getStatus(),
                lotes.size(), quantidadeGuias);
    }
}
