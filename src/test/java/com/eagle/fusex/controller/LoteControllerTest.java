package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.LoteResponse;
import com.eagle.fusex.service.LoteService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
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
class LoteControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoteService loteService;

    @Test
    @DisplayName("POST /lotes - Deve retornar 401 UNAUTHORIZED quando sem credenciais")
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        MockMultipartFile fatura = new MockMultipartFile("fatura", "fatura.pdf", "application/pdf", new byte[]{1});
        MockMultipartFile espelho = new MockMultipartFile("espelho", "espelho.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/lotes").file(fatura).file(espelho).param("guiaIds", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /lotes - Deve retornar 403 FORBIDDEN para papel não autorizado")
    void deveRetornar403ParaPapelNaoAutorizado() throws Exception {
        MockMultipartFile fatura = new MockMultipartFile("fatura", "fatura.pdf", "application/pdf", new byte[]{1});
        MockMultipartFile espelho = new MockMultipartFile("espelho", "espelho.pdf", "application/pdf", new byte[]{1});

        mockMvc.perform(multipart("/lotes")
                        .file(fatura).file(espelho)
                        .param("guiaIds", "1")
                        .with(httpBasic("medico", "senha123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /lotes - Deve gerar lote com papel OCS")
    void deveGerarLoteComPapelOcs() throws Exception {
        MockMultipartFile fatura = new MockMultipartFile("fatura", "fatura.pdf", "application/pdf", new byte[]{1});
        MockMultipartFile espelho = new MockMultipartFile("espelho", "espelho.pdf", "application/pdf", new byte[]{1});
        LoteResponse response = new LoteResponse(1L, 1, 2026, 10, LocalDateTime.now(),
                "fatura.pdf", "espelho.pdf", 2);
        when(loteService.gerarLote(any(), any(), any())).thenReturn(response);

        mockMvc.perform(multipart("/lotes")
                        .file(fatura).file(espelho)
                        .param("guiaIds", "1", "2")
                        .with(httpBasic("ocs", "senha123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroLote").value(1))
                .andExpect(jsonPath("$.quantidadeGuias").value(2));
    }

    @Test
    @DisplayName("GET /lotes - Deve listar lotes da OCS logada")
    void deveListarLotesComPapelOcs() throws Exception {
        LoteResponse response = new LoteResponse(1L, 1, 2026, 10, LocalDateTime.now(),
                "fatura.pdf", "espelho.pdf", 2);
        when(loteService.listarLotes()).thenReturn(List.of(response));

        mockMvc.perform(get("/lotes").with(httpBasic("ocs", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroLote").value(1));
    }
}
