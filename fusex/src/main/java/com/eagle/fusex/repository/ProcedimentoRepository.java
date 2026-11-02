package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.model.enums.Especialidade;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ProcedimentoRepository extends JpaRepository<Procedimento, String> {
    List<Procedimento> findTop20ByProcDescricaoContainingIgnoreCaseOrderByProcDescricao(String busca);
    Page<Procedimento> findByProcDescricaoContainingIgnoreCaseOrderByProcDescricao(String busca, Pageable pageable);

    @Query("""
        SELECT DISTINCT p FROM Procedimento p
        JOIN p.ocsProcedimentos op
        JOIN op.ocs o
        JOIN o.especialidades esp
        WHERE (LOWER(p.procDescricao) LIKE LOWER(CONCAT('%', :busca, '%'))
           OR p.procCodigoDgp LIKE CONCAT('%', :busca, '%'))
          AND esp IN (:especialidades)
        ORDER BY p.procDescricao ASC
    """)
    List<Procedimento> findTop20ByDescricaoAndEspecialidades(
        @Param("busca") String busca,
        @Param("especialidades") Collection<Especialidade> especialidades
    );

    @Query("""
        SELECT DISTINCT p FROM Procedimento p
        JOIN p.ocsProcedimentos op
        JOIN op.ocs o
        JOIN o.especialidades esp
        WHERE esp IN (:especialidades)
        ORDER BY p.procDescricao ASC
    """)
    List<Procedimento> findDistinctByEspecialidades(
        @Param("especialidades") Collection<Especialidade> especialidades
    );

    @Query("""
        SELECT p FROM Procedimento p
        WHERE LOWER(p.procDescricao) LIKE LOWER(CONCAT('%', :busca, '%'))
           OR p.procCodigoDgp LIKE CONCAT('%', :busca, '%')
        ORDER BY p.procDescricao ASC
    """)
    List<Procedimento> findTop50ByDescricaoOuCodigo(
        @Param("busca") String busca
    );
}
