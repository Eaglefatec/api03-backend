package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Encaminhamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface EncaminhamentoRepository extends JpaRepository<Encaminhamento, Long> {

    Optional<Encaminhamento> findFirstBySolicitacaoMedicaIdOrderByDataHoraDesc(Long solicitacaoId);

    boolean existsBySolicitacaoMedicaId(Long solicitacaoId);

    List<Encaminhamento> findBySolicitacaoMedicaIdIn(Collection<Long> solicitacaoIds);
}
