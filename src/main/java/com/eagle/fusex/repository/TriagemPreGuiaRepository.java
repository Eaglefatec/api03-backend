package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.TriagemPreGuia;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TriagemPreGuiaRepository extends JpaRepository<TriagemPreGuia, Long> {
    Optional<TriagemPreGuia> findBySolicitacaoMedicaId(Long solicitacaoId);
}
