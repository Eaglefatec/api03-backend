package com.eagle.fusex.service;

import com.eagle.fusex.model.dto.request.CadastrarPacienteRequest;
import com.eagle.fusex.model.dto.response.PacienteResponse;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.repository.PacienteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PacienteServiceTest {

    @Mock
    private PacienteRepository pacienteRepository;

    @InjectMocks
    private PacienteService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    @DisplayName("Deve cadastrar paciente novo com CPF, PREC e E-mail")
    void deveCadastrarPacienteNovoComSucesso() {
        CadastrarPacienteRequest request = new CadastrarPacienteRequest(
                "Tenente Ferreira", "1º BLog", "111.222.333-44", "PREC-9988", "ferreira@eb.mil.br"
        );

        when(pacienteRepository.findByCpfOrPrec(any(), any())).thenReturn(Optional.empty());
        when(pacienteRepository.save(any(Paciente.class))).thenAnswer(invocation -> {
            Paciente p = invocation.getArgument(0);
            p.setId(10L);
            return p;
        });

        PacienteResponse response = service.cadastrarPaciente(request);

        assertNotNull(response);
        assertEquals(10L, response.getId());
        assertEquals("Tenente Ferreira", response.getNome());
        assertEquals("11122233344", response.getCpf());
        assertEquals("PREC-9988", response.getPrec());
        assertEquals("ferreira@eb.mil.br", response.getEmail());
    }
}

