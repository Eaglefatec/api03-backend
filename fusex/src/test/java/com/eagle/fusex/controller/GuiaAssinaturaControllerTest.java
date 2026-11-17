package com.eagle.fusex.controller;

import com.eagle.fusex.model.dto.request.DecisaoAssinaturaRequest;
import com.eagle.fusex.model.dto.response.GuiaAssinaturaResponse;
import com.eagle.fusex.model.enums.StatusEmail;
import com.eagle.fusex.model.enums.StatusEsteiraAssinatura;
import com.eagle.fusex.service.GuiaAssinaturaService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GuiaAssinaturaControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private GuiaAssinaturaService guiaAssinaturaService;

    private GuiaAssinaturaResponse criarResponseExemplo(Long id, String numero, StatusEsteiraAssinatura status) {
        return new GuiaAssinaturaResponse(
                id,
                numero,
                "Marcos V. Andrade",
                "34766899402",
                "marcos@email.com.br",
                "Hospital Policlin S/A",
                "guia_" + numero + ".pdf",
                20480L,
                status,
                status.getCodigoCurto(),
                status.getDescricao(),
                LocalDateTime.now(),
                null,
                null,
                LocalDate.now().plusDays(60),
                null,
                StatusEmail.SIMULADO,
                "Mensagem pronta",
                "Operação realizada"
        );
    }

    @Test
    @DisplayName("POST /assinaturas/guias - Deve retornar 401 quando sem credenciais")
    void deveRetornar401QuandoSemCredenciais() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1, 2});

        mockMvc.perform(multipart("/assinaturas/guias").file(arquivo))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /assinaturas/guias - Deve retornar 403 para papel OCS não autorizado a enviar guia")
    void deveRetornar403ParaOcsNoUpload() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia.pdf", "application/pdf", new byte[]{1, 2});

        mockMvc.perform(multipart("/assinaturas/guias")
                        .file(arquivo)
                        .with(httpBasic("ocs", "senha123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST /assinaturas/guias - Deve permitir upload pelo Emissor de Guias")
    void devePermitirUploadComPapelEg() throws Exception {
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia_202610700.pdf", "application/pdf", new byte[]{1, 2, 3});
        GuiaAssinaturaResponse resp = criarResponseExemplo(1L, "202610700", StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);
        when(guiaAssinaturaService.enviarGuiaParaAssinatura(any())).thenReturn(resp);

        mockMvc.perform(multipart("/assinaturas/guias")
                        .file(arquivo)
                        .with(httpBasic("eg", "senha123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroGuia").value("202610700"))
                .andExpect(jsonPath("$.statusCodigo").value("AM"));
    }

    @Test
    @DisplayName("GET /assinaturas/guias/pendencias - Deve permitir consulta pelo Médico Chefe e Chefe da Seção")
    void deveListarPendenciasParaMedicoChefeEChefeSecao() throws Exception {
        GuiaAssinaturaResponse respAm = criarResponseExemplo(1L, "202610700", StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);
        when(guiaAssinaturaService.listarPendencias(eq("med"), any())).thenReturn(List.of(respAm));

        mockMvc.perform(get("/assinaturas/guias/pendencias")
                        .param("perfil", "med")
                        .with(httpBasic("medicochefe", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].numeroGuia").value("202610700"))
                .andExpect(jsonPath("$[0].statusCodigo").value("AM"));
    }

    @Test
    @DisplayName("POST /assinaturas/guias/{id}/decisao - Deve registrar assinatura com papel Chefe da Seção")
    void deveRegistrarDecisaoComChefeSecao() throws Exception {
        GuiaAssinaturaResponse respPe = criarResponseExemplo(3L, "202610702", StatusEsteiraAssinatura.AGUARDANDO_ENVIO_BENEFICIARIO);
        when(guiaAssinaturaService.processarDecisao(eq(3L), any(DecisaoAssinaturaRequest.class), any())).thenReturn(respPe);

        String payload = """
                {
                  "decisao": "APROVAR",
                  "perfil": "sec",
                  "confirmado": true,
                  "assinaturaBase64": "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg=="
                }
                """;

        mockMvc.perform(post("/assinaturas/guias/3/decisao")
                        .with(httpBasic("chefesecao", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroGuia").value("202610702"))
                .andExpect(jsonPath("$.statusCodigo").value("PE"));
    }

    @Test
    @DisplayName("POST /assinaturas/guias/{id}/enviar-email - Deve disparar envio ao beneficiário com papel EG")
    void deveEnviarEmailBeneficiarioComPapelEg() throws Exception {
        GuiaAssinaturaResponse respEn = criarResponseExemplo(4L, "202610703", StatusEsteiraAssinatura.ENVIADA_BENEFICIARIO);
        when(guiaAssinaturaService.enviarEmailBeneficiario(4L)).thenReturn(respEn);

        mockMvc.perform(post("/assinaturas/guias/4/enviar-email")
                        .with(httpBasic("eg", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroGuia").value("202610703"))
                .andExpect(jsonPath("$.statusCodigo").value("EN"));
    }

    @Test
    @DisplayName("GET /assinaturas/pendencias - Médico solicitante comum deve receber 403 ao tentar acessar fila do Médico Chefe")
    void deveRetornar403ParaMedicoComumNaFilaDeAssinatura() throws Exception {
        mockMvc.perform(get("/assinaturas/pendencias")
                        .with(httpBasic("medico", "senha123")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Deve retornar 400 quando parte do arquivo estiver ausente ou JSON for inválido")
    void deveRetornar400QuandoParteArquivoAusenteOuJsonInvalido() throws Exception {
        mockMvc.perform(multipart("/assinaturas/guias")
                        .with(httpBasic("eg", "senha123")))
                .andExpect(status().isBadRequest());

        mockMvc.perform(post("/assinaturas/guias/3/decisao")
                        .with(httpBasic("chefesecao", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decisao\": \"VALOR_INEXISTENTE\"}"))
                .andExpect(status().isBadRequest());
    }
}
