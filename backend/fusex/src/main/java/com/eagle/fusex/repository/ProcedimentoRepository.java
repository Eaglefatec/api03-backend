package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Procedimento;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProcedimentoRepository extends JpaRepository<Procedimento, String> {
    List<Procedimento> findTop20ByProcDescricaoContainingIgnoreCaseOrderByProcDescricao(String busca);
    Page<Procedimento> findByProcDescricaoContainingIgnoreCaseOrderByProcDescricao(String busca, Pageable pageable);
}
