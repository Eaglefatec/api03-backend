package com.eagle.fusex.service;

import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.model.dto.request.CadastrarPacienteRequest;
import com.eagle.fusex.model.dto.response.PacienteResponse;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.repository.PacienteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class PacienteService {

    private final PacienteRepository pacienteRepository;

    public PacienteService(PacienteRepository pacienteRepository) {
        this.pacienteRepository = pacienteRepository;
    }

    @Transactional(readOnly = true)
    public List<PacienteResponse> buscarPacientes(String busca) {
        if (busca == null || busca.trim().length() < 2) {
            return Collections.emptyList();
        }

        return pacienteRepository
                .findTop10ByNomeContainingIgnoreCaseOrderByNome(busca.trim())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public PacienteResponse cadastrarPaciente(CadastrarPacienteRequest request) {
        Paciente paciente = pacienteRepository.findByCpfPrec(request.getCpfPrec().trim())
                .orElseGet(() -> new Paciente(
                        request.getNome().trim(),
                        request.getCpfPrec().trim(),
                        request.getOm().trim(),
                        request.getIdade(),
                        request.getTelefone()
                ));

        paciente.setNome(request.getNome().trim());
        paciente.setOm(request.getOm().trim());
        if (request.getIdade() != null) {
            paciente.setIdade(request.getIdade());
        }
        if (request.getTelefone() != null && !request.getTelefone().isBlank()) {
            paciente.setTelefone(request.getTelefone().trim());
        }

        Paciente salvo = pacienteRepository.save(paciente);
        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public Paciente buscarPorId(Long id) {
        return pacienteRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Paciente", id));
    }

    private PacienteResponse toResponse(Paciente p) {
        return new PacienteResponse(p.getId(), p.getNome(), p.getOm(), p.getCpfPrec(), p.getIdade(), p.getTelefone());
    }
}
