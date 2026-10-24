package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.Medico;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class MedicoRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private MedicoRepository medicoRepository;

    @Test
    @DisplayName("findByIdAndCredenciadoTrue - Deve retornar médico quando for credenciado")
    void deveRetornarMedicoCredenciado() {
        Medico medico = entityManager.persist(new Medico("Dr. Fernando", "CRM9988", true));
        entityManager.flush();

        Optional<Medico> resultado = medicoRepository.findByIdAndCredenciadoTrue(medico.getId());

        assertTrue(resultado.isPresent());
        assertEquals("Dr. Fernando", resultado.get().getNome());
        assertTrue(resultado.get().getCredenciado());
    }

    @Test
    @DisplayName("findByIdAndCredenciadoTrue - Não deve retornar médico quando não for credenciado")
    void naoDeveRetornarMedicoNaoCredenciado() {
        Medico medico = entityManager.persist(new Medico("Dr. NaoCredenciado", "CRM7766", false));
        entityManager.flush();

        Optional<Medico> resultado = medicoRepository.findByIdAndCredenciadoTrue(medico.getId());

        assertTrue(resultado.isEmpty());
    }
}
