package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Medico;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MedicoRepository extends JpaRepository<Medico, Long> {
    Optional<Medico> findByIdAndCredenciadoTrue(Long id);
    Optional<Medico> findByCrm(String crm);
    Optional<Medico> findByCrmAndCredenciadoTrue(String crm);
}
