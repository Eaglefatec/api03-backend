package com.eagle.fusex.controller;

import com.eagle.fusex.exception.SolicitacaoNaoEncontradaException;
import com.eagle.fusex.model.dto.request.PreencherTriagemRequest;
import com.eagle.fusex.model.dto.response.SolicitacaoPublicaResponse;
import com.eagle.fusex.model.dto.response.TriagemResponse;
import com.eagle.fusex.model.enums.Especialidade;
import com.eagle.fusex.service.TriagemPreGuiaService;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SolicitacaoPublicoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TriagemPreGuiaService triagemPreGuiaService;

    @Test
    @DisplayName("GET /solicitacoes/publico/{token} - Deve retornar 200 OK sem necessidade de autenticação")
    void deveBuscarSolicitacaoPorTokenComSucesso() throws Exception {
        SolicitacaoPublicaResponse response = new SolicitacaoPublicaResponse(
                "Carlos Oliveira", "OM-01", Set.of(Especialidade.CARDIOLOGISTA), "Observacao teste", false
        );

        when(triagemPreGuiaService.buscarPorToken("token-valido-123")).thenReturn(response);

        mockMvc.perform(get("/solicitacoes/publico/token-valido-123")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomePaciente").value("Carlos Oliveira"))
                .andExpect(jsonPath("$.om").value("OM-01"))
                .andExpect(jsonPath("$.triagemPreenchida").value(false));
    }

    @Test
    @DisplayName("GET /solicitacoes/publico/{token} - Deve retornar 404 NOT_FOUND quando solicitação não existir")
    void deveRetornar404QuandoTokenNaoExistir() throws Exception {
        when(triagemPreGuiaService.buscarPorToken("token-inexistente"))
                .thenThrow(new SolicitacaoNaoEncontradaException("Solicitação não encontrada"));

        mockMvc.perform(get("/solicitacoes/publico/token-inexistente")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Solicitação não encontrada"));
    }

    @Test
    @DisplayName("POST /solicitacoes/publico/{token}/triagem - Deve retornar 201 CREATED com dados válidos")
    void devePreencherTriagemComSucesso() throws Exception {
        PreencherTriagemRequest request = new PreencherTriagemRequest();
        request.setCpfPrec("12345678900");
        request.setIdade(45);
        request.setTelefone("11999998888");
        request.setOcsId(1L);
        request.setAceitoTermos(true);

        TriagemResponse response = new TriagemResponse("Triagem realizada com sucesso", List.of());

        when(triagemPreGuiaService.preencherTriagem(eq("token-valido-123"), any(PreencherTriagemRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/solicitacoes/publico/token-valido-123/triagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.mensagem").value("Triagem realizada com sucesso"));
    }

    @Test
    @DisplayName("POST /solicitacoes/publico/{token}/triagem - Deve retornar 400 BAD_REQUEST quando dados forem inválidos")
    void deveRetornar400QuandoDadosInvalidos() throws Exception {
        PreencherTriagemRequest request = new PreencherTriagemRequest();
        // Campos obrigatórios ausentes propositalmente

        mockMvc.perform(post("/solicitacoes/publico/token-valido-123/triagem")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Erro de validação"))
                .andExpect(jsonPath("$.campos").isMap());
    }
}
