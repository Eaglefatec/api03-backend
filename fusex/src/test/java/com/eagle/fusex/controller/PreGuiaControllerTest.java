package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.PreGuiaResumoResponse;
import com.eagle.fusex.model.enums.StatusPreGuia;
import com.eagle.fusex.service.PreGuiaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PreGuiaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private PreGuiaService preGuiaService;

    @Test
    @DisplayName("GET /pre-guias - Deve retornar 401 sem credenciais")
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/pre-guias"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /pre-guias - Deve retornar 403 para OCS e Faturamento")
    void deveRetornar403ParaPapeisNaoAutorizados() throws Exception {
        mockMvc.perform(get("/pre-guias").with(httpBasic("ocs", "senha123")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/pre-guias").with(httpBasic("faturamento", "senha123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /pre-guias?status=PENDENTE - Deve listar para o EG")
    void deveListarPendentesParaEg() throws Exception {
        when(preGuiaService.listar(StatusPreGuia.PENDENTE)).thenReturn(List.of(new PreGuiaResumoResponse(
                1L, "token-1", "MARIA DE OLIVEIRA", "33333333333", "CLINICA SANTA LUCIA",
                LocalDateTime.of(2026, 11, 10, 14, 30), null, true, false, null, null)));

        mockMvc.perform(get("/pre-guias").param("status", "PENDENTE").with(httpBasic("eg", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].token").value("token-1"))
                .andExpect(jsonPath("$[0].possuiEncaminhamento").value(true));
    }
}
