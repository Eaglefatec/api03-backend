package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.enums.Especialidade;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OcsRepository extends JpaRepository<Ocs, Long> {
    Optional<Ocs> findByOcsNome(String ocsNome);
    Optional<Ocs> findByOcsInscricaoFederal(String ocsInscricaoFederal);
    List<Ocs> findDistinctByEspecialidadesIn(Set<Especialidade> especialidades);
    Optional<Ocs> findByLoginUsuario(String loginUsuario);
    Optional<Ocs> findFirstByOrderByOcsIdAsc();

    @Query("""
        SELECT o FROM Ocs o
        WHERE o.ocsId IN (
            SELECT op.ocs.ocsId FROM OcsProcedimento op
            WHERE op.procedimento.procCodigoDgp IN (:codigos)
            GROUP BY op.ocs.ocsId
            HAVING COUNT(DISTINCT op.procedimento.procCodigoDgp) = :totalProcedimentos
        )
        ORDER BY o.ocsNome ASC
    """)
    List<Ocs> findOcsQueAtendemTodosProcedimentos(
        @Param("codigos") Collection<String> codigos,
        @Param("totalProcedimentos") long totalProcedimentos
    );
}
