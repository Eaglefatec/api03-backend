package com.eagle.fusex.service;

import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.model.dto.response.ProcedimentoResponse;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.repository.ProcedimentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Service
public class ProcedimentoService {

    private final ProcedimentoRepository procedimentoRepository;

    public ProcedimentoService(ProcedimentoRepository procedimentoRepository) {
        this.procedimentoRepository = procedimentoRepository;
    }

    @Transactional(readOnly = true)
    public List<ProcedimentoResponse> listarProcedimentos(String busca) {
        if (busca == null || busca.isBlank()) {
            return Collections.emptyList();
        }

        return procedimentoRepository
                .findTop20ByProcDescricaoContainingIgnoreCaseOrderByProcDescricao(busca.trim())
                .stream()
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
