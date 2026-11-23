package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.enums.StatusEmail;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SolicitacaoMedicaRepository extends JpaRepository<SolicitacaoMedica, Long> {

    @EntityGraph(attributePaths = {"paciente", "medico", "medicoResponsavel"})
    Optional<SolicitacaoMedica> findByTokenPublico(String tokenPublico);

    List<SolicitacaoMedica> findByMedicoIdAndPacienteIdAndValidaTrue(Long medicoId, Long pacienteId);

    @EntityGraph(attributePaths = {"paciente", "medico", "medicoResponsavel"})
    Optional<SolicitacaoMedica> findByNumeroGuia(String numeroGuia);

    @EntityGraph(attributePaths = {"paciente", "medico", "medicoResponsavel"})
    List<SolicitacaoMedica> findByStatusEmailAndEmailProximaTentativaLessThanEqualAndEmailTentativasLessThan(
            StatusEmail statusEmail,
            LocalDateTime agora,
            Integer maxTentativas);
}
