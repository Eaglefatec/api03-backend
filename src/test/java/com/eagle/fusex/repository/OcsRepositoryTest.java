package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.enums.Especialidade;
import com.eagle.fusex.model.enums.TipoOcs;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class OcsRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private OcsRepository ocsRepository;

    @Test
    @DisplayName("findDistinctByEspecialidadesIn - Deve retornar clínica sem duplicação quando possui múltiplas especialidades correspondentes")
    void deveRetornarClinicaSemDuplicacao() {
        Ocs ocs = new Ocs();
        ocs.setOcsNome("Hospital Geral Teste");
        ocs.setOcsTipo(TipoOcs.OCS);
        ocs.setEspecialidades(Set.of(Especialidade.CARDIOLOGISTA, Especialidade.ORTOPEDISTA));

        entityManager.persist(ocs);
        entityManager.flush();

        List<Ocs> resultado = ocsRepository.findDistinctByEspecialidadesIn(
                Set.of(Especialidade.CARDIOLOGISTA, Especialidade.ORTOPEDISTA)
        );

        assertNotNull(resultado);
        assertEquals(1, resultado.size(), "Não deve haver duplicação de OCS na busca com múltiplas especialidades");
        assertEquals("Hospital Geral Teste", resultado.get(0).getOcsNome());
    }
}
