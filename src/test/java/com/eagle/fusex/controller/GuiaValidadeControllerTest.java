package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.GuiaValidadeResponse;
import com.eagle.fusex.service.GuiaValidadeService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GuiaValidadeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuiaValidadeService guiaValidadeService;

    @Test
    @DisplayName("GET /guias/{numeroGuia}/validade - Deve retornar 401 UNAUTHORIZED quando sem credenciais")
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/guias/ABC-123/validade"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /guias/{numeroGuia}/validade - Deve retornar 403 FORBIDDEN para papel não autorizado")
    void deveRetornar403ParaPapelNaoAutorizado() throws Exception {
        mockMvc.perform(get("/guias/ABC-123/validade")
                        .with(httpBasic("medico", "senha123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /guias/{numeroGuia}/validade - Deve retornar 200 OK para papel OCS")
    void deveConsultarValidadeComPapelOcs() throws Exception {
        GuiaValidadeResponse response = new GuiaValidadeResponse(
                "ABC-123", true, null, null, null, "Paciente Teste", "Clínica Teste");
        when(guiaValidadeService.consultarValidade("ABC-123")).thenReturn(response);

        mockMvc.perform(get("/guias/ABC-123/validade")
                        .with(httpBasic("ocs", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida").value(true))
                .andExpect(jsonPath("$.nomePaciente").value("Paciente Teste"))
                .andExpect(jsonPath("$.ocsVinculada").value("Clínica Teste"));
    }

    @Test
    @DisplayName("GET /guias/{numeroGuia}/validade - Deve retornar motivo quando guia inválida")
    void deveRetornarMotivoQuandoGuiaInvalida() throws Exception {
        GuiaValidadeResponse response = new GuiaValidadeResponse(
                "ABC-123", false, "GUIA_EXPIRADA", null, null, "Paciente Teste", "Clínica Teste");
        when(guiaValidadeService.consultarValidade("ABC-123")).thenReturn(response);

        mockMvc.perform(get("/guias/ABC-123/validade")
                        .with(httpBasic("ocs", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valida").value(false))
                .andExpect(jsonPath("$.motivoInvalidez").value("GUIA_EXPIRADA"));
    }
}
