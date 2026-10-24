package com.eagle.fusex.controller;

import com.eagle.fusex.model.entity.Medico;
import com.eagle.fusex.repository.MedicoRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MedicoRepository medicoRepository;

    @Test
    void testMe_ComUsuarioMedico_RetornaPerfilEMedico() throws Exception {
        Medico medico = new Medico("Dr. João Silva", "123456", true);
        medico.setId(1L);

        when(medicoRepository.findByIdAndCredenciadoTrue(1L)).thenReturn(Optional.of(medico));

        mockMvc.perform(get("/auth/me")
                        .with(user("medico").roles("MEDICO")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("medico"))
                .andExpect(jsonPath("$.medico.id").value(1))
                .andExpect(jsonPath("$.medico.nome").value("Dr. João Silva"))
                .andExpect(jsonPath("$.medico.crm").value("123456"));
    }

    @Test
    void testMe_SemAutenticacao_RetornaUnauthorized() throws Exception {
        mockMvc.perform(get("/auth/me"))
                .andExpect(status().isUnauthorized());
    }
}
