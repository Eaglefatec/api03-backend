package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.GuiaImportacaoResponse;
import com.eagle.fusex.service.GuiaImportacaoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GuiaImportacaoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuiaImportacaoService guiaImportacaoService;

    @Test
    @DisplayName("POST /solicitacoes/{token}/guia-pdf - Deve retornar 401 UNAUTHORIZED quando sem credenciais")
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/solicitacoes/token-1/guia-pdf").file(arquivo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /solicitacoes/{token}/guia-pdf - Deve retornar 403 FORBIDDEN para papel não autorizado")
    void deveRetornar403ParaPapelNaoAutorizado() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/solicitacoes/token-1/guia-pdf")
                        .file(arquivo)
                        .with(httpBasic("ocs", "senha123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /solicitacoes/{token}/guia-pdf - Deve importar com papel EG")
    void deveImportarPdfComPapelEg() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1});
        GuiaImportacaoResponse response = new GuiaImportacaoResponse(
                "ABC-123", null, null, true, "Guia importada e número identificado automaticamente");
        when(guiaImportacaoService.importarPdf(eq("token-1"), any())).thenReturn(response);

        mockMvc.perform(multipart("/solicitacoes/token-1/guia-pdf")
                        .file(arquivo)
                        .with(httpBasic("eg", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroGuia").value("ABC-123"))
                .andExpect(jsonPath("$.numeroIdentificado").value(true));
    }

    @Test
    @DisplayName("PATCH /solicitacoes/{token}/guia-pdf/numero - Deve corrigir número manualmente")
    void deveCorrigirNumeroGuiaComPapelEg() throws Exception {
        GuiaImportacaoResponse response = new GuiaImportacaoResponse(
                "XYZ-999", null, null, true, "Número da guia atualizado com sucesso");
        when(guiaImportacaoService.corrigirNumeroGuia("token-1", "XYZ-999")).thenReturn(response);

        mockMvc.perform(patch("/solicitacoes/token-1/guia-pdf/numero")
                        .with(httpBasic("eg", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroGuia\":\"XYZ-999\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroGuia").value("XYZ-999"));
    }
}
