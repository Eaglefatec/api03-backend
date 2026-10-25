package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Guia;
import com.eagle.fusex.model.enums.StatusGuia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface GuiaRepository extends JpaRepository<Guia, Long> {
    List<Guia> findByOcs_LoginUsuarioOrderByCreatedAtDesc(String loginUsuario);
    List<Guia> findByOcs_LoginUsuarioAndStatusOrderByCreatedAtDesc(String loginUsuario, StatusGuia status);
    boolean existsByNumeroGuia(String numeroGuia);
    long countByLoteId(Long loteId);


    @Query("SELECT g FROM Guia g WHERE g.ocs.ocsId = :ocsId")
    List<Guia> findByOcs_Id(@Param("ocsId") Long ocsId);
}