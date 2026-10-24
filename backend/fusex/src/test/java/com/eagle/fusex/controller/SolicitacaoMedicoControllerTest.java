package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.request.CriarSolicitacaoRequest;
import com.eagle.fusex.model.dto.response.PreGuiaConsolidadaResponse;
import com.eagle.fusex.model.dto.response.SolicitacaoResponse;
import com.eagle.fusex.model.enums.Especialidade;
import com.eagle.fusex.service.PreGuiaConsolidadaService;
import com.eagle.fusex.service.SolicitacaoMedicaService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SolicitacaoMedicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private SolicitacaoMedicaService solicitacaoMedicaService;

    @MockitoBean
    private PreGuiaConsolidadaService preGuiaConsolidadaService;

    @Test
    @DisplayName("POST /solicitacoes - Deve retornar 401 UNAUTHORIZED quando sem credenciais")
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        CriarSolicitacaoRequest request = new CriarSolicitacaoRequest();

        mockMvc.perform(post("/solicitacoes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /solicitacoes - Deve criar solicitação e retornar 201 CREATED com credenciais válidas")
    void deveCriarSolicitacaoComSucesso() throws Exception {
        CriarSolicitacaoRequest.ProcedimentoQuantidade pq = new CriarSolicitacaoRequest.ProcedimentoQuantidade();
        pq.setProcedimentoCodigoDgp("DGP001");
        pq.setQuantidade(1);

        CriarSolicitacaoRequest request = new CriarSolicitacaoRequest();
        request.setMedicoId(1L);
        request.setNomePaciente("João Silva");
        request.setOm("OM-01");
        request.setCpfPrec("12345678901");
        request.setEspecialidades(Set.of(Especialidade.CARDIOLOGISTA));
        request.setProcedimentos(List.of(pq));

        SolicitacaoResponse response = new SolicitacaoResponse();
        response.setId(5L);
        response.setTokenPublico("token-gerado-abc");
        response.setLinkBeneficiario("/solicitacoes/publico/token-gerado-abc");

        when(solicitacaoMedicaService.criar(any(CriarSolicitacaoRequest.class))).thenReturn(response);

        mockMvc.perform(post("/solicitacoes")
                        .with(httpBasic("medico", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.tokenPublico").value("token-gerado-abc"))
                .andExpect(jsonPath("$.linkBeneficiario").value("/solicitacoes/publico/token-gerado-abc"));
    }

    @Test
    @DisplayName("POST /solicitacoes - Deve retornar 400 BAD REQUEST quando validação de DTO falhar")
    void deveRetornar400QuandoValidacaoFalhar() throws Exception {
        CriarSolicitacaoRequest request = new CriarSolicitacaoRequest();
        // Campos obrigatórios vazios

        mockMvc.perform(post("/solicitacoes")
                        .with(httpBasic("medico", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Erro de validação"))
                .andExpect(jsonPath("$.campos").isMap());
    }

    @Test
    @DisplayName("GET /solicitacoes/{token}/pre-guia - Deve retornar 401 UNAUTHORIZED quando sem credenciais")
    void deveBloquearConsultaPreGuiaSemAutenticacao() throws Exception {
        mockMvc.perform(get("/solicitacoes/token-123/pre-guia"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /solicitacoes/{token}/pre-guia - Deve retornar 200 OK quando autenticado")
    void deveConsultarPreGuiaQuandoAutenticado() throws Exception {
        PreGuiaConsolidadaResponse response = new PreGuiaConsolidadaResponse();
        response.setBeneficiario(new PreGuiaConsolidadaResponse.Beneficiario("João Silva", 40, "12345678901", "11999998888"));

        when(preGuiaConsolidadaService.consultar("token-123")).thenReturn(response);

        mockMvc.perform(get("/solicitacoes/token-123/pre-guia")
                        .with(httpBasic("medico", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beneficiario.nome").value("João Silva"))
                .andExpect(jsonPath("$.beneficiario.cpfPrec").value("12345678901"));
    }
}
