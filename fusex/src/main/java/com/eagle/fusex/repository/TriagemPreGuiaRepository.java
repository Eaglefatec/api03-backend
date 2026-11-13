package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.TriagemPreGuia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface TriagemPreGuiaRepository extends JpaRepository<TriagemPreGuia, Long> {
    Optional<TriagemPreGuia> findBySolicitacaoMedicaId(Long solicitacaoId);

    @Query("""
            select t from TriagemPreGuia t
            join fetch t.solicitacaoMedica s
            join fetch s.paciente
            join fetch t.ocs
            where s.valida = true
            """)
    List<TriagemPreGuia> findPreGuiasDeSolicitacoesValidas();
}
