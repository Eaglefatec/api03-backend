package com.eagle.fusex.service;

import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.model.dto.response.MedicoResumoResponse;
import com.eagle.fusex.model.entity.Medico;
import com.eagle.fusex.repository.MedicoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class MedicoService {

    private final MedicoRepository medicoRepository;

    public MedicoService(MedicoRepository medicoRepository) {
        this.medicoRepository = medicoRepository;
    }

    @Transactional(readOnly = true)
    public Medico buscarPorId(Long id) {
        return medicoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médico", id));
    }

    @Transactional(readOnly = true)
    public Medico buscarCredenciadoPorId(Long id) {
        return medicoRepository.findByIdAndCredenciadoTrue(id)
                .orElseThrow(() -> new ResourceNotFoundException("Médico credenciado", id));
    }

    @Transactional(readOnly = true)
    public Optional<Medico> buscarPorCrm(String crm) {
        return medicoRepository.findByCrm(crm);
    }

    @Transactional(readOnly = true)
    public List<MedicoResumoResponse> listarTodos() {
        return medicoRepository.findAll().stream()
                .map(MedicoResumoResponse::from)
                .toList();
    }
}
