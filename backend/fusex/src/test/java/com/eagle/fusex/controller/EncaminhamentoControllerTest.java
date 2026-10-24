package com.eagle.fusex.controller;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.model.dto.response.EncaminhamentoResponse;
import com.eagle.fusex.service.EncaminhamentoService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class EncaminhamentoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private EncaminhamentoService encaminhamentoService;

    @Test
    @DisplayName("POST /encaminhamentos - Deve retornar 401 quando não autenticado")
    void deveRetornar401QuandoNaoAutenticado() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "exame.pdf", "application/pdf", "conteudo".getBytes()
        );

        mockMvc.perform(multipart("/encaminhamentos")
                        .file(arquivo)
                        .param("medicoId", "1"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /encaminhamentos - Deve fazer upload com sucesso quando autenticado")
    void deveFazerUploadQuandoAutenticado() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "exame.pdf", "application/pdf", "conteudo".getBytes()
        );

        EncaminhamentoResponse response = new EncaminhamentoResponse(1L, LocalDateTime.now(), "Encaminhamento realizado com sucesso");

        when(encaminhamentoService.enviarEncaminhamento(eq(1L), any())).thenReturn(response);

        mockMvc.perform(multipart("/encaminhamentos")
                        .file(arquivo)
                        .param("medicoId", "1")
                        .with(httpBasic("medico", "senha123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.mensagem").value("Encaminhamento realizado com sucesso"));
    }

    @Test
    @DisplayName("POST /encaminhamentos - Deve retornar 400 quando o arquivo for inválido")
    void deveRetornar400QuandoArquivoInvalido() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile(
                "arquivo", "arquivo.exe", "application/x-msdownload", "conteudo".getBytes()
        );

        when(encaminhamentoService.enviarEncaminhamento(eq(1L), any()))
                .thenThrow(new ArquivoInvalidoException("Extensão de arquivo não permitida. Apenas PDF, PNG e JPG são aceitos."));

        mockMvc.perform(multipart("/encaminhamentos")
                        .file(arquivo)
                        .param("medicoId", "1")
                        .with(httpBasic("medico", "senha123")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.error").value("Extensão de arquivo não permitida. Apenas PDF, PNG e JPG são aceitos."));
    }
}
