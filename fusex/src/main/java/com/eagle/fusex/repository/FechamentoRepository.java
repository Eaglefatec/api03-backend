package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Fechamento;
import com.eagle.fusex.model.enums.StatusFechamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FechamentoRepository extends JpaRepository<Fechamento, Long> {
    List<Fechamento> findByOcs_LoginUsuarioOrderByDataEnvioDesc(String loginUsuario);
    List<Fechamento> findByStatusOrderByDataEnvioAsc(StatusFechamento status);
    boolean existsByOcs_OcsIdAndCompetenciaAnoAndCompetenciaMes(Long ocsId, Integer competenciaAno, Integer competenciaMes);
}
