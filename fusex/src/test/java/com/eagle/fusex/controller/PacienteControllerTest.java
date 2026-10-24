package com.eagle.fusex.controller;

import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.repository.PacienteRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class PacienteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PacienteRepository pacienteRepository;

    @Test
    void testBuscarPacientes_ComTermoValido() throws Exception {
        Paciente p = new Paciente("Sargento Carlos Silva", "11122233344", "12º GAC");
        p.setId(10L);

        when(pacienteRepository.findTop10ByNomeContainingIgnoreCaseOrderByNome("carlos"))
                .thenReturn(List.of(p));

        mockMvc.perform(get("/pacientes?busca=carlos")
                        .with(user("medico").roles("MEDICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(10))
                .andExpect(jsonPath("$[0].nome").value("Sargento Carlos Silva"))
                .andExpect(jsonPath("$[0].om").value("12º GAC"))
                .andExpect(jsonPath("$[0].cpfPrec").value("11122233344"));
    }

    @Test
    void testBuscarPacientes_SemTermo_RetornaVazio() throws Exception {
        mockMvc.perform(get("/pacientes")
                        .with(user("medico").roles("MEDICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void testBuscarPacientes_SemAutenticacao_RetornaUnauthorized() throws Exception {
        mockMvc.perform(get("/pacientes?busca=carlos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void testCadastrarPaciente_ComDadosValidos() throws Exception {
        Paciente p = new Paciente("Capitão Souza", "55566677788", "Cmdo 2ª RM", 40, "(11) 98888-7777");
        p.setId(20L);

        when(pacienteRepository.findByCpfPrec("55566677788")).thenReturn(java.util.Optional.empty());
        when(pacienteRepository.save(any(Paciente.class))).thenReturn(p);

        String json = """
                {
                    "nome": "Capitão Souza",
                    "om": "Cmdo 2ª RM",
                    "cpfPrec": "55566677788",
                    "idade": 40,
                    "telefone": "(11) 98888-7777"
                }
                """;

        mockMvc.perform(post("/pacientes")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json)
                        .with(user("medico").roles("MEDICO")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(20))
                .andExpect(jsonPath("$.nome").value("Capitão Souza"))
                .andExpect(jsonPath("$.om").value("Cmdo 2ª RM"))
                .andExpect(jsonPath("$.cpfPrec").value("55566677788"))
                .andExpect(jsonPath("$.idade").value(40))
                .andExpect(jsonPath("$.telefone").value("(11) 98888-7777"));
    }

    @Test
    void testCadastrarPaciente_SemAutenticacao_RetornaUnauthorized() throws Exception {
        String json = """
                {
                    "nome": "Capitão Souza",
                    "om": "Cmdo 2ª RM",
                    "cpfPrec": "55566677788"
                }
                """;

        mockMvc.perform(post("/pacientes")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized());
    }
}
