package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.FechamentoResponse;
import com.eagle.fusex.model.enums.StatusFechamento;
import com.eagle.fusex.service.FechamentoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FechamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FechamentoService fechamentoService;

    @Test
    @DisplayName("POST /fechamentos - Deve retornar 401 UNAUTHORIZED quando sem credenciais")
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        mockMvc.perform(post("/fechamentos")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competenciaAno\":2026,\"competenciaMes\":10}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /fechamentos - Deve retornar 403 FORBIDDEN para papel não autorizado")
    void deveRetornar403ParaPapelNaoAutorizado() throws Exception {
        mockMvc.perform(post("/fechamentos")
                        .with(httpBasic("medico", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competenciaAno\":2026,\"competenciaMes\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /fechamentos - Deve enviar fechamento com papel OCS")
    void deveEnviarFechamentoComPapelOcs() throws Exception {
        FechamentoResponse response = new FechamentoResponse(1L, 2026, 10, LocalDateTime.now(),
                StatusFechamento.AGUARDANDO_FATURAMENTO, 2, 5);
        when(fechamentoService.enviarFechamento(2026, 10)).thenReturn(response);

        mockMvc.perform(post("/fechamentos")
                        .with(httpBasic("ocs", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"competenciaAno\":2026,\"competenciaMes\":10}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_FATURAMENTO"))
                .andExpect(jsonPath("$.quantidadeLotes").value(2));
    }

    @Test
    @DisplayName("GET /fechamentos - Deve listar fechamentos da OCS logada")
    void deveListarFechamentosComPapelOcs() throws Exception {
        FechamentoResponse response = new FechamentoResponse(1L, 2026, 10, LocalDateTime.now(),
                StatusFechamento.AGUARDANDO_FATURAMENTO, 2, 5);
        when(fechamentoService.listarFechamentos()).thenReturn(List.of(response));

        mockMvc.perform(get("/fechamentos").with(httpBasic("ocs", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].competenciaMes").value(10));
    }
}
