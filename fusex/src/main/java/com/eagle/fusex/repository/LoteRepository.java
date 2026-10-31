package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Lote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LoteRepository extends JpaRepository<Lote, Long> {
    long countByOcs_OcsIdAndCompetenciaAnoAndCompetenciaMes(Long ocsId, Integer competenciaAno, Integer competenciaMes);
    List<Lote> findByOcs_LoginUsuarioAndFechamentoIsNullOrderByDataGeracaoDesc(String loginUsuario);
    List<Lote> findByOcs_OcsIdAndCompetenciaAnoAndCompetenciaMesAndFechamentoIsNull(
            Long ocsId, Integer competenciaAno, Integer competenciaMes);
    List<Lote> findByFechamentoId(Long fechamentoId);
    boolean existsByNumeroSisfat(String numeroSisfat);
}
