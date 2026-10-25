package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.GuiaItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface GuiaItemRepository extends JpaRepository<GuiaItem, Long> {
    List<GuiaItem> findByGuiaId(Long guiaId);
}