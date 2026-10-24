package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.enums.Especialidade;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface OcsRepository extends JpaRepository<Ocs, Long> {
    Optional<Ocs> findByOcsNome(String ocsNome);
    List<Ocs> findDistinctByEspecialidadesIn(Set<Especialidade> especialidades);
    Optional<Ocs> findByLoginUsuario(String loginUsuario);
    Optional<Ocs> findFirstByOrderByOcsIdAsc();
}
