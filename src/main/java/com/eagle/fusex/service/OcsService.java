package com.eagle.fusex.service;

import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.model.dto.response.OcsResponse;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.OcsProcedimento;
import com.eagle.fusex.model.enums.Especialidade;
import com.eagle.fusex.repository.OcsProcedimentoRepository;
import com.eagle.fusex.repository.OcsRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;

@Service
public class OcsService {

    private final OcsRepository ocsRepository;
    private final OcsProcedimentoRepository ocsProcedimentoRepository;

    public OcsService(OcsRepository ocsRepository, OcsProcedimentoRepository ocsProcedimentoRepository) {
        this.ocsRepository = ocsRepository;
        this.ocsProcedimentoRepository = ocsProcedimentoRepository;
    }

    @Transactional(readOnly = true)
    public List<OcsResponse> listarPorProcedimento(String procedimentoId) {
        return ocsProcedimentoRepository.findByProcedimento_ProcCodigoDgp(procedimentoId).stream()
                .map(this::toOcsResponseComValor)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OcsResponse> listarPorEspecialidades(Set<Especialidade> especialidades) {
        return ocsRepository.findDistinctByEspecialidadesIn(especialidades).stream()
                .map(OcsResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OcsResponse> listarTodas() {
        return ocsRepository.findAll().stream()
                .map(OcsResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Ocs buscarPorId(Long id) {
        return ocsRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("OCS", id));
    }

    private OcsResponse toOcsResponseComValor(OcsProcedimento ocsProcedimento) {
        Ocs ocs = ocsProcedimento.getOcs();
        OcsResponse response = OcsResponse.from(ocs);
        response.setValor(ocsProcedimento.getValor());
        response.setTabelaReferencia(ocsProcedimento.getTabelaReferencia());
        return response;
    }
}
