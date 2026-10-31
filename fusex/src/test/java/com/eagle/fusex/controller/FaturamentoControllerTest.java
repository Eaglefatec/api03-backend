package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.dto.response.FaturamentoFechamentoResponse;
import com.eagle.fusex.model.enums.StatusFechamento;
import com.eagle.fusex.service.FaturamentoService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class FaturamentoControllerTest {

    private static final String CORPO_VALIDO = "{\"lotes\":[{\"loteId\":1,\"numeroSisfat\":\"SF-1\"}]}";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private FaturamentoService faturamentoService;

    private FaturamentoFechamentoResponse fechamentoResponse(StatusFechamento status) {
        return new FaturamentoFechamentoResponse(10L, "Plani Diagnósticos", 2026, 10,
                LocalDateTime.now(), 1, 2, status);
    }

    @Test
    @DisplayName("GET /faturamento/fechamentos - Deve retornar 401 sem credenciais")
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/faturamento/fechamentos"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /faturamento/fechamentos - Deve retornar 403 para a OCS")
    void deveRetornar403ParaOcs() throws Exception {
        mockMvc.perform(get("/faturamento/fechamentos").with(httpBasic("ocs", "senha123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /faturamento/fechamentos - Deve listar para o Faturamento")
    void deveListarFechamentosParaFaturamento() throws Exception {
        when(faturamentoService.listarFechamentosPendentes())
                .thenReturn(List.of(fechamentoResponse(StatusFechamento.AGUARDANDO_FATURAMENTO)));

        mockMvc.perform(get("/faturamento/fechamentos").with(httpBasic("faturamento", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ocsNome").value("Plani Diagnósticos"))
                .andExpect(jsonPath("$[0].status").value("AGUARDANDO_FATURAMENTO"));
    }

    @Test
    @DisplayName("POST /faturamento/fechamentos/{id}/confirmar - Deve retornar 403 para a Lisura/OCS")
    void deveRetornar403AoConfirmarComOcs() throws Exception {
        mockMvc.perform(post("/faturamento/fechamentos/10/confirmar")
                        .with(httpBasic("ocs", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /faturamento/fechamentos/{id}/confirmar - Deve confirmar com o Faturamento")
    void deveConfirmarComFaturamento() throws Exception {
        when(faturamentoService.confirmar(any(), any()))
                .thenReturn(fechamentoResponse(StatusFechamento.AGUARDANDO_LISURA));

        mockMvc.perform(post("/faturamento/fechamentos/10/confirmar")
                        .with(httpBasic("faturamento", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(CORPO_VALIDO))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AGUARDANDO_LISURA"));
    }

    @Test
    @DisplayName("POST /faturamento/fechamentos/{id}/confirmar - Deve retornar 400 sem lotes")
    void deveRetornar400SemLotes() throws Exception {
        mockMvc.perform(post("/faturamento/fechamentos/10/confirmar")
                        .with(httpBasic("faturamento", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"lotes\":[]}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /faturamento/lotes/{id}/fatura - Deve devolver o PDF inline")
    void deveVisualizarFatura() throws Exception {
        when(faturamentoService.obterFatura(1L))
                .thenReturn(new ArquivoLoteDownload(new byte[]{37, 80, 68, 70}, "fatura.pdf", "application/pdf"));

        mockMvc.perform(get("/faturamento/lotes/1/fatura").with(httpBasic("faturamento", "senha123")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
