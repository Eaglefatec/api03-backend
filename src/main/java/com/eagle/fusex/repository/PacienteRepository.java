package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    Optional<Paciente> findByCpfPrec(String cpfPrec);
    List<Paciente> findTop10ByNomeContainingIgnoreCaseOrderByNome(String nome);
}
