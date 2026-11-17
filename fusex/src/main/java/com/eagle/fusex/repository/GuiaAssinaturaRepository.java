package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.GuiaAssinatura;
import com.eagle.fusex.model.enums.StatusEsteiraAssinatura;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface GuiaAssinaturaRepository extends JpaRepository<GuiaAssinatura, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT g FROM GuiaAssinatura g WHERE g.id = :id")
    Optional<GuiaAssinatura> findByIdForUpdate(@Param("id") Long id);

    List<GuiaAssinatura> findAllByOrderByDataEnvioDesc();

    List<GuiaAssinatura> findByStatusOrderByDataEnvioAsc(StatusEsteiraAssinatura status);

    List<GuiaAssinatura> findByStatusOrderByDataAssinaturaSecaoDesc(StatusEsteiraAssinatura status);

    List<GuiaAssinatura> findByStatusOrderByDataEnvioEmailDesc(StatusEsteiraAssinatura status);

    Optional<GuiaAssinatura> findByNumeroGuia(String numeroGuia);

    boolean existsByNumeroGuia(String numeroGuia);

    long countByStatus(StatusEsteiraAssinatura status);
}

