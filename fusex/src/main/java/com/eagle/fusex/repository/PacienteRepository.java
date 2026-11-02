package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    Optional<Paciente> findByCpf(String cpf);
    Optional<Paciente> findByPrec(String prec);

    @Query("SELECT p FROM Paciente p WHERE p.cpf = :cpf OR p.prec = :prec")
    Optional<Paciente> findByCpfOrPrec(@Param("cpf") String cpf, @Param("prec") String prec);

    default Optional<Paciente> findByCpfPrec(String cpfPrec) {
        String normalizado = Paciente.normalizarCpf(cpfPrec);
        return findByCpfOrPrec(normalizado, cpfPrec);
    }

    List<Paciente> findTop10ByNomeContainingIgnoreCaseOrderByNome(String nome);
}
