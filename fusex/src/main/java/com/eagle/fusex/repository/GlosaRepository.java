package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Glosa;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface GlosaRepository extends JpaRepository<Glosa, Long> {
    List<Glosa> findByGuiaId(Long guiaId);
}