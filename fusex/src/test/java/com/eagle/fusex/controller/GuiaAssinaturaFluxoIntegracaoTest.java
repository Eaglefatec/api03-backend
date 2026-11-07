package com.eagle.fusex.controller;

import com.eagle.fusex.model.entity.GuiaAssinatura;
import com.eagle.fusex.repository.GuiaAssinaturaRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.apache.pdfbox.text.PDFTextStripper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class GuiaAssinaturaFluxoIntegracaoTest {

    private static final String ASSINATURA_PNG_BASE64 =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GuiaAssinaturaRepository guiaAssinaturaRepository;

    private byte[] criarPdfGuiaReal(String numero, String beneficiario, String prec, String ocs, String email) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.newLineAtOffset(40, 720);
                cs.showText("GUIA DE ENCAMINHAMENTO Nr: " + numero);
                cs.newLineAtOffset(0, -20);
                cs.showText("Beneficiario: " + beneficiario);
                cs.newLineAtOffset(0, -20);
                cs.showText("Prec-CP: " + prec);
                cs.newLineAtOffset(0, -20);
                cs.showText("Prestador (OCS): " + ocs);
                cs.newLineAtOffset(0, -20);
                cs.showText("E-mail: " + email);
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("Fluxo completo: upload da guia, dupla assinatura com validade de 60 dias e envio por e-mail")
    void deveExecutarCicloCompletoDaEsteiraDeAssinaturaEEnvio() throws Exception {
        // 1. Upload do PDF pelo Emissor de Guias
        byte[] pdfBytes = criarPdfGuiaReal(
                "202610999",
                "Cap. Renato G. Vasconcelos",
                "34999111222",
                "Hospital Policlin S/A",
                "renato.vasconcelos@email.com.br"
        );
        MockMultipartFile arquivoPdf = new MockMultipartFile(
                "arquivo", "guia_202610999.pdf", "application/pdf", pdfBytes);

        MvcResult uploadResult = mockMvc.perform(multipart("/assinaturas/guias")
                        .file(arquivoPdf)
                        .with(httpBasic("eg", "senha123")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroGuia").value("202610999"))
                .andExpect(jsonPath("$.beneficiarioNome").value("Cap. Renato G. Vasconcelos"))
                .andExpect(jsonPath("$.ocsNome").value("Hospital Policlin S/A"))
                .andExpect(jsonPath("$.statusCodigo").value("AM"))
                .andExpect(jsonPath("$.statusDescricao").value("Aguardando Médico Chefe"))
                .andReturn();

        JsonNode guiaCriada = objectMapper.readTree(uploadResult.getResponse().getContentAsString());
        long guiaId = guiaCriada.get("id").asLong();

        mockMvc.perform(get("/assinaturas/guias")
                        .with(httpBasic("eg", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + guiaId + ")].statusCodigo").value(contains("AM")))
                .andExpect(jsonPath("$[?(@.id == " + guiaId + ")].statusDescricao").value(contains("Aguardando Médico Chefe")));

        // 2. Primeira assinatura pelo Médico Chefe
        mockMvc.perform(get("/assinaturas/pendencias")
                        .param("perfil", "med")
                        .with(httpBasic("medicochefe", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].statusCodigo", everyItem(is("AM"))))
                .andExpect(jsonPath("$[?(@.id == " + guiaId + ")].numeroGuia").value(contains("202610999")));

        mockMvc.perform(get("/assinaturas/guias/" + guiaId + "/detalhes")
                        .with(httpBasic("medicochefe", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroGuia").value("202610999"))
                .andExpect(jsonPath("$.beneficiarioNome").value("Cap. Renato G. Vasconcelos"))
                .andExpect(jsonPath("$.ocsNome").value("Hospital Policlin S/A"))
                .andExpect(jsonPath("$.regraValidade").value("60 dias a partir da assinatura do Chefe da Seção"))
                .andExpect(jsonPath("$.procedimentos").isArray());

        mockMvc.perform(get("/assinaturas/guias/" + guiaId + "/pdf")
                        .with(httpBasic("medicochefe", "senha123")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF));

        String payloadSemConfirmacao = """
                {
                  "decisao": "APROVAR",
                  "perfil": "med",
                  "confirmado": false,
                  "assinaturaBase64": "%s"
                }
                """.formatted(ASSINATURA_PNG_BASE64);

        mockMvc.perform(post("/assinaturas/guias/" + guiaId + "/decisao")
                        .with(httpBasic("medicochefe", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadSemConfirmacao))
                .andExpect(status().isBadRequest());

        String payloadAssinaturaMedico = """
                {
                  "decisao": "APROVAR",
                  "perfil": "med",
                  "confirmado": true,
                  "assinaturaBase64": "%s"
                }
                """.formatted(ASSINATURA_PNG_BASE64);

        mockMvc.perform(post("/assinaturas/guias/" + guiaId + "/decisao")
                        .with(httpBasic("medicochefe", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadAssinaturaMedico))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCodigo").value("AS"))
                .andExpect(jsonPath("$.statusDescricao").value("Aguardando Chefe da Seção"))
                .andExpect(jsonPath("$.dataAssinaturaMedico").isNotEmpty());

        // 3. Segunda assinatura pelo Chefe da Seção e conferência do PDF gerado
        mockMvc.perform(get("/assinaturas/pendencias")
                        .param("perfil", "sec")
                        .with(httpBasic("chefesecao", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].statusCodigo", everyItem(is("AS"))))
                .andExpect(jsonPath("$[?(@.id == " + guiaId + ")].numeroGuia").value(contains("202610999")));

        mockMvc.perform(get("/assinaturas/guias/" + guiaId + "/detalhes")
                        .with(httpBasic("chefesecao", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.assinaturaMedicoBase64").value(ASSINATURA_PNG_BASE64))
                .andExpect(jsonPath("$.dataAssinaturaMedico").isNotEmpty());

        String payloadAssinaturaSecao = """
                {
                  "decisao": "APROVAR",
                  "perfil": "sec",
                  "confirmado": true,
                  "assinaturaBase64": "%s"
                }
                """.formatted(ASSINATURA_PNG_BASE64);

        String validadeEsperada = LocalDate.now().plusDays(60).toString();

        mockMvc.perform(post("/assinaturas/guias/" + guiaId + "/decisao")
                        .with(httpBasic("chefesecao", "senha123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payloadAssinaturaSecao))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCodigo").value("PE"))
                .andExpect(jsonPath("$.statusDescricao").value("Aguardando envio ao beneficiário"))
                .andExpect(jsonPath("$.dataAssinaturaSecao").isNotEmpty())
                .andExpect(jsonPath("$.dataValidade").value(validadeEsperada));

        GuiaAssinatura entidadeBanco = guiaAssinaturaRepository.findById(guiaId).orElseThrow();
        assertNotNull(entidadeBanco.getCaminhoArquivoOriginal());
        assertNotNull(entidadeBanco.getCaminhoArquivoAssinado());
        assertTrue(Files.exists(Path.of(entidadeBanco.getCaminhoArquivoOriginal())), "PDF original deve existir em disco");
        assertTrue(Files.exists(Path.of(entidadeBanco.getCaminhoArquivoAssinado())), "PDF assinado deve existir em disco");

        MvcResult pdfAssinadoResult = mockMvc.perform(get("/assinaturas/guias/" + guiaId + "/pdf")
                        .with(httpBasic("eg", "senha123")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn();

        byte[] bytesPdfAssinado = pdfAssinadoResult.getResponse().getContentAsByteArray();
        try (PDDocument docAssinado = Loader.loadPDF(bytesPdfAssinado)) {
            assertEquals(1, docAssinado.getNumberOfPages());
            String textoPdfAssinado = new PDFTextStripper().getText(docAssinado);
            assertTrue(textoPdfAssinado.contains("202610999"), "Deve preservar o número da guia no PDF assinado");
            assertTrue(textoPdfAssinado.contains("Medico Chefe FUSEX"), "Deve conter chancela do Médico Chefe");
            assertTrue(textoPdfAssinado.contains("Chefe da Secao FUSEX"), "Deve conter chancela do Chefe da Seção");
            assertTrue(textoPdfAssinado.contains("Valida por 60 dias a partir da assinatura do Chefe da Secao"),
                    "Deve conter a validade de 60 dias estampada no PDF");
            assertTrue(docAssinado.getPage(0).getResources().getXObjectNames().iterator().hasNext(),
                    "Deve conter as imagens PNG das rubricas embutidas como XObject no PDF");
        }

        mockMvc.perform(get("/guias/202610999/validade")
                        .with(httpBasic("ocs", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroGuia").value("202610999"))
                .andExpect(jsonPath("$.valida").value(true))
                .andExpect(jsonPath("$.dataValidade").value(validadeEsperada));

        // 4. Envio por e-mail ao beneficiário
        mockMvc.perform(get("/assinaturas/prontas-envio")
                        .with(httpBasic("eg", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + guiaId + ")].statusCodigo").value(contains("PE")))
                .andExpect(jsonPath("$[?(@.id == " + guiaId + ")].beneficiarioEmail").value(contains("renato.vasconcelos@email.com.br")))
                .andExpect(jsonPath("$[?(@.id == " + guiaId + ")].mensagemPronta").value(contains(containsString("Guia nº 202610999"))));

        mockMvc.perform(post("/assinaturas/guias/" + guiaId + "/enviar-email")
                        .with(httpBasic("eg", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statusCodigo").value("EN"))
                .andExpect(jsonPath("$.statusDescricao").value("Enviada ao beneficiário"))
                .andExpect(jsonPath("$.dataEnvioEmail").isNotEmpty());

        mockMvc.perform(get("/assinaturas/enviadas")
                        .with(httpBasic("eg", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + guiaId + ")].statusCodigo").value(contains("EN")))
                .andExpect(jsonPath("$[?(@.id == " + guiaId + ")].beneficiarioEmail").value(contains("renato.vasconcelos@email.com.br")));

        mockMvc.perform(get("/assinaturas/guias")
                        .with(httpBasic("eg", "senha123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id == " + guiaId + ")].statusCodigo").value(contains("EN")))
                .andExpect(jsonPath("$[?(@.id == " + guiaId + ")].statusDescricao").value(contains("Enviada ao beneficiário")));
    }
}
