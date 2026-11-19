package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.response.DivergenciaResponse;
import com.eagle.fusex.model.dto.response.FaturamentoLoteResponse;
import com.eagle.fusex.model.entity.GuiaItem;
import com.eagle.fusex.model.enums.StatusLote;
import com.eagle.fusex.service.LisuraService;
import com.eagle.fusex.service.RelatorioDivergenciaPdfService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class LisuraControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LisuraService lisuraService;

    @MockitoBean
    private RelatorioDivergenciaPdfService relatorioDivergenciaPdfService;

    @Test
    @DisplayName("GET /lisura/fechamentos - Deve exigir autenticação (401) e restringir à role ADMIN (403)")
    void deveRestringirAcessoApenasParaAdmin() throws Exception {
        mockMvc.perform(get("/lisura/fechamentos"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/lisura/fechamentos")
                        .with(httpBasic("ocs@gmail.com", "senha123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("GET /lisura/fechamentos - Deve listar fechamentos formatados para ADMIN")
    void deveListarFechamentosParaAdmin() throws Exception {
        when(lisuraService.listarFechamentosFormatados()).thenReturn(List.of(
                Map.of("id", 1L, "competenciaMes", 10, "competenciaAno", 2026,
                        "ocs", Map.of("ocsId", 5L, "ocsNome", "Hospital Teste"))
        ));

        mockMvc.perform(get("/lisura/fechamentos")
                        .with(httpBasic("admin@gmail.com", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].ocs.ocsNome").value("Hospital Teste"));
    }

    @Test
    @DisplayName("GET /lisura/fechamento/{id}/lotes - Deve retornar DTOs de lotes sem proxies lazy")
    void deveListarLotesPorFechamento() throws Exception {
        when(lisuraService.listarLotesPorFechamento(1L)).thenReturn(List.of(
                new FaturamentoLoteResponse(10L, 1, 3L, "fatura.pdf", "espelho.xlsx", "SF-001", StatusLote.AGUARDANDO_LISURA)
        ));

        mockMvc.perform(get("/lisura/fechamento/1/lotes")
                        .with(httpBasic("admin@gmail.com", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroSisfat").value("SF-001"))
                .andExpect(jsonPath("$[0].status").value("AGUARDANDO_LISURA"));
    }

    @Test
    @DisplayName("GET /lisura/cruzamento/{id} e /guia-itens/{id} - Devem retornar divergências e itens para ADMIN")
    void deveExecutarCruzamentoEListarItens() throws Exception {
        when(lisuraService.executarCruzamentoDados(1L)).thenReturn(List.of(
                new DivergenciaResponse(20L, "G-20", "40301010", "João Silva", "12345",
                        new BigDecimal("150.00"), new BigDecimal("100.00"), new BigDecimal("50.00"), "DIVERGENTE")
        ));

        GuiaItem item = new GuiaItem();
        item.setId(99L);
        item.setGuiaId(20L);
        item.setCodigoTuss("40301010");
        item.setDescricaoItem("Hemograma completo");
        item.setTipo("Procedimento");
        item.setQuantidade(1);
        item.setValorUnitario(new BigDecimal("150.00"));
        item.setDataItem(LocalDate.of(2026, 10, 15));
        when(lisuraService.listarItensDaGuia(20L)).thenReturn(List.of(item));

        mockMvc.perform(get("/lisura/cruzamento/1")
                        .with(httpBasic("admin@gmail.com", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroGuia").value("G-20"))
                .andExpect(jsonPath("$[0].statusDivergencia").value("DIVERGENTE"))
                .andExpect(jsonPath("$[0].diferenca").value(50.00));

        mockMvc.perform(get("/lisura/guia-itens/20")
                        .with(httpBasic("admin@gmail.com", "admin123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigoTuss").value("40301010"))
                .andExpect(jsonPath("$[0].descricaoItem").value("Hemograma completo"));
    }

    @Test
    @DisplayName("POST /lisura/glosas e GET /lisura/relatorio/{id} - Devem validar payload e retornar PDF")
    void deveRegistrarGlosaEGerarRelatorioPdf() throws Exception {
        mockMvc.perform(post("/lisura/glosas")
                        .with(httpBasic("admin@gmail.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());

        String glosaJson = """
                {
                  "guiaId": 20,
                  "codigoTuss": "40301010",
                  "justificativaMilitar": "Valor acima da tabela contratada",
                  "valorGlosa": 50.00
                }
                """;

        mockMvc.perform(post("/lisura/glosas")
                        .with(httpBasic("admin@gmail.com", "admin123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(glosaJson))
                .andExpect(status().isOk());

        verify(lisuraService).registrarGlosa(any());

        when(relatorioDivergenciaPdfService.gerarRelatorioDivergenciasPdf(1L))
                .thenReturn(new byte[]{37, 80, 68, 70});

        mockMvc.perform(get("/lisura/relatorio/1")
                        .with(httpBasic("admin@gmail.com", "admin123")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));
    }
}
