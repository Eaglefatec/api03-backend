package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.GuiaResponse;
import com.eagle.fusex.model.enums.StatusGuia;
import com.eagle.fusex.service.GuiaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GuiaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuiaService guiaService;

    @Test
    @DisplayName("POST /guias - Deve retornar 401 UNAUTHORIZED quando sem credenciais")
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/guias")
                        .file(arquivo)
                        .param("numeroGuia", "202610606")
                        .param("beneficiarioPrecCp", "34596112300")
                        .param("dataAtendimento", "2026-10-02"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /guias - Deve retornar 403 FORBIDDEN para papel não autorizado")
    void deveRetornar403ParaPapelNaoAutorizado() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/guias")
                        .file(arquivo)
                        .param("numeroGuia", "202610606")
                        .param("beneficiarioPrecCp", "34596112300")
                        .param("dataAtendimento", "2026-10-02")
                        .with(httpBasic("medico", "senha123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /guias - Deve anexar guia com papel OCS")
    void deveAnexarGuiaComPapelOcs() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1});
        GuiaResponse response = new GuiaResponse(1L, "202610606", "34596112300",
                LocalDate.of(2026, 10, 2), "guia.pdf", StatusGuia.SEM_LOTE);
        when(guiaService.anexarGuia(any(), any(), any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/guias")
                        .file(arquivo)
                        .param("numeroGuia", "202610606")
                        .param("beneficiarioPrecCp", "34596112300")
                        .param("dataAtendimento", "2026-10-02")
                        .with(httpBasic("ocs", "senha123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroGuia").value("202610606"))
                .andExpect(jsonPath("$.status").value("SEM_LOTE"));
    }

    @Test
    @DisplayName("GET /guias - Deve listar guias da OCS logada")
    void deveListarGuiasComPapelOcs() throws Exception {
        GuiaResponse response = new GuiaResponse(1L, "202610606", "34596112300",
                LocalDate.of(2026, 10, 2), "guia.pdf", StatusGuia.SEM_LOTE);
        when(guiaService.listarGuias()).thenReturn(List.of(response));

        mockMvc.perform(get("/guias").with(httpBasic("ocs", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroGuia").value("202610606"));
    }
}
