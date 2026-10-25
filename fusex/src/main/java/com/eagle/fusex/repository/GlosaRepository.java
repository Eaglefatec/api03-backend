package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Glosa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface GlosaRepository extends JpaRepository<Glosa, Long> {
    List<Glosa> findByGuiaId(Long guiaId);
}