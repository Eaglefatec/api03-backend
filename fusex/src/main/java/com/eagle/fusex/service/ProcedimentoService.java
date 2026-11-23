package com.eagle.fusex.service;

import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.model.dto.response.ProcedimentoResponse;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.model.enums.Especialidade;
import com.eagle.fusex.repository.ProcedimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

@Service
public class ProcedimentoService {

    private final ProcedimentoRepository procedimentoRepository;

    public ProcedimentoService(ProcedimentoRepository procedimentoRepository) {
        this.procedimentoRepository = procedimentoRepository;
    }

    @Transactional(readOnly = true)
    public List<ProcedimentoResponse> listarProcedimentos(String busca) {
        return listarProcedimentos(busca, null);
    }

    @Transactional(readOnly = true)
    public List<ProcedimentoResponse> listarProcedimentos(String busca, Set<Especialidade> especialidades) {
        boolean temBusca = busca != null && !busca.isBlank();
        boolean temEspecialidades = especialidades != null && !especialidades.isEmpty();

        List<Procedimento> procedimentos;
        if (temBusca && temEspecialidades) {
            procedimentos = procedimentoRepository.findTop20ByDescricaoAndEspecialidades(busca.trim(), especialidades);
        } else if (temBusca) {
            procedimentos = procedimentoRepository.findTop50ByDescricaoOuCodigo(busca.trim());
        } else if (temEspecialidades) {
            procedimentos = procedimentoRepository.findDistinctByEspecialidades(especialidades);
        } else {
            return Collections.emptyList();
        }

        return procedimentos.stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Procedimento buscarPorCodigo(String procCodigoDgp) {
        return procedimentoRepository.findById(procCodigoDgp)
                .orElseThrow(() -> new ResourceNotFoundException("Procedimento", procCodigoDgp));
    }

    @Transactional(readOnly = true)
    public List<Procedimento> buscarTodosPorCodigos(Collection<String> codigos) {
        return procedimentoRepository.findAllById(codigos);
    }

    private ProcedimentoResponse toResponse(Procedimento p) {
        return new ProcedimentoResponse(p.getProcCodigoDgp(), p.getProcDescricao());
    }
}
