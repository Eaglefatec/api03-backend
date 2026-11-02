package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.OcsProcedimento;
import com.eagle.fusex.model.entity.OcsProcedimentoId;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface OcsProcedimentoRepository extends JpaRepository<OcsProcedimento, OcsProcedimentoId> {
    List<OcsProcedimento> findByProcedimento_ProcCodigoDgp(String procCodigoDgp);
    Optional<OcsProcedimento> findByOcs_OcsIdAndProcedimento_ProcCodigoDgp(Long ocsId, String procCodigoDgp);
    List<OcsProcedimento> findByOcs_OcsIdAndProcedimento_ProcCodigoDgpIn(Long ocsId, Collection<String> procCodigosDgp);
}
