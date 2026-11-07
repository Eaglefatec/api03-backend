package com.eagle.fusex.service;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.exception.UnauthorizedOperationException;
import com.eagle.fusex.exception.ValidationException;
import com.eagle.fusex.model.dto.ResultadoEnvioEmail;
import com.eagle.fusex.model.dto.request.DecisaoAssinaturaRequest;
import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.dto.response.GuiaAssinaturaDetalheResponse;
import com.eagle.fusex.model.dto.response.GuiaAssinaturaResponse;
import com.eagle.fusex.model.entity.GuiaAssinatura;
import com.eagle.fusex.model.enums.DecisaoAssinatura;
import com.eagle.fusex.model.enums.StatusEmail;
import com.eagle.fusex.model.enums.StatusEsteiraAssinatura;
import com.eagle.fusex.repository.GuiaAssinaturaRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.repository.PacienteRepository;
import com.eagle.fusex.repository.SolicitacaoMedicaRepository;
import com.eagle.fusex.storage.FileStorageService;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class GuiaAssinaturaServiceTest {

    private static final String ASSINATURA_PNG_BASE64 =
            "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNk+M9QDwADhgGAWjR9awAAAABJRU5ErkJggg==";

    @Mock
    private GuiaAssinaturaRepository guiaAssinaturaRepository;

    @Mock
    private PacienteRepository pacienteRepository;

    @Mock
    private OcsRepository ocsRepository;

    @Mock
    private SolicitacaoMedicaRepository solicitacaoRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private GuiaAssinaturaEmailService emailService;

    private GuiaAssinaturaPdfService pdfService;
    private GuiaAssinaturaService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        pdfService = new GuiaAssinaturaPdfService("(?i)guia\\s+de\\s+encaminhamento\\s+n[ºr°.]*\\s*:?\\s*([A-Z0-9./-]{4,})");
        service = new GuiaAssinaturaService(
                guiaAssinaturaRepository,
                pacienteRepository,
                ocsRepository,
                solicitacaoRepository,
                fileStorageService,
                pdfService,
                emailService,
                60
        );
    }

    private byte[] gerarPdfGuiaValido(String numeroGuia, String beneficiario, String prec, String ocs) throws IOException {
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage();
            doc.addPage(page);
            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                cs.beginText();
                cs.setFont(new PDType1Font(Standard14Fonts.FontName.HELVETICA), 11);
                cs.newLineAtOffset(40, 720);
                cs.showText("GUIA DE ENCAMINHAMENTO Nr: " + numeroGuia);
                cs.newLineAtOffset(0, -20);
                cs.showText("Beneficiario: " + beneficiario);
                cs.newLineAtOffset(0, -20);
                cs.showText("Prec-CP: " + prec);
                cs.newLineAtOffset(0, -20);
                cs.showText("Prestador (OCS): " + ocs);
                cs.endText();
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    @Test
    @DisplayName("Deve receber PDF de até 5 MB, extrair os dados automaticamente e criar a guia com status AM")
    void deveEnviarGuiaComExtracaoAutomaticaStatusAM() throws Exception {
        byte[] pdfBytes = gerarPdfGuiaValido("202610888", "Carlos Eduardo Rocha", "34911223344", "Hospital Policlin S/A");
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "guia_202610888.pdf", "application/pdf", pdfBytes);

        when(guiaAssinaturaRepository.existsByNumeroGuia("202610888")).thenReturn(false);
        when(fileStorageService.salvar(any(byte[].class), anyString())).thenReturn("uploads/guia_202610888.pdf");
        when(guiaAssinaturaRepository.save(any(GuiaAssinatura.class))).thenAnswer(inv -> {
            GuiaAssinatura g = inv.getArgument(0);
            g.setId(10L);
            return g;
        });

        GuiaAssinaturaResponse resp = service.enviarGuiaParaAssinatura(arquivo);

        assertNotNull(resp);
        assertEquals(10L, resp.id());
        assertEquals("202610888", resp.numeroGuia());
        assertEquals("Carlos Eduardo Rocha", resp.beneficiarioNome());
        assertEquals("34911223344", resp.beneficiarioPrecCp());
        assertEquals("Hospital Policlin S/A", resp.ocsNome());
        assertEquals(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE, resp.status());
        assertEquals("AM", resp.statusCodigo());
        assertEquals("Aguardando Médico Chefe", resp.statusDescricao());
    }

    @Test
    @DisplayName("Deve rejeitar upload de arquivo maior que 5 MB")
    void deveRejeitarArquivoMaiorQue5Mb() {
        byte[] seisMb = new byte[(int) (GuiaAssinaturaService.LIMITE_TAMANHO_PDF_BYTES + 1024)];
        MockMultipartFile arquivoGrande = new MockMultipartFile("arquivo", "grande.pdf", "application/pdf", seisMb);

        ArquivoInvalidoException ex = assertThrows(ArquivoInvalidoException.class,
                () -> service.enviarGuiaParaAssinatura(arquivoGrande));
        assertTrue(ex.getMessage().contains("5 MB"));
    }

    @Test
    @DisplayName("Deve rejeitar upload de arquivo que não seja PDF")
    void deveRejeitarArquivoNaoPdf() {
        MockMultipartFile arquivoTxt = new MockMultipartFile("arquivo", "documento.txt", "text/plain", "conteudo".getBytes());

        assertThrows(ArquivoInvalidoException.class,
                () -> service.enviarGuiaParaAssinatura(arquivoTxt));
    }

    @Test
    @DisplayName("Deve filtrar pendências por perfil: Médico Chefe vê apenas AM e Chefe da Seção vê apenas AS")
    void deveListarPendenciasFiltradasPorPerfil() {
        GuiaAssinatura guiaAm = new GuiaAssinatura();
        guiaAm.setId(1L);
        guiaAm.setNumeroGuia("202610700");
        guiaAm.setBeneficiarioNome("Marcos V. Andrade");
        guiaAm.setOcsNome("Hospital Policlin S/A");
        guiaAm.setStatus(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);

        GuiaAssinatura guiaAs = new GuiaAssinatura();
        guiaAs.setId(2L);
        guiaAs.setNumeroGuia("202610702");
        guiaAs.setBeneficiarioNome("Roberto C. Mendes");
        guiaAs.setOcsNome("Laboratório Oswaldo Cruz");
        guiaAs.setStatus(StatusEsteiraAssinatura.AGUARDANDO_CHEFE_SECAO);

        when(guiaAssinaturaRepository.findByStatusOrderByDataEnvioAsc(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE))
                .thenReturn(List.of(guiaAm));
        when(guiaAssinaturaRepository.findByStatusOrderByDataEnvioAsc(StatusEsteiraAssinatura.AGUARDANDO_CHEFE_SECAO))
                .thenReturn(List.of(guiaAs));

        Authentication authMed = new UsernamePasswordAuthenticationToken(
                "medicochefe", "senha123", List.of(new SimpleGrantedAuthority("ROLE_MEDICO_CHEFE")));
        Authentication authSec = new UsernamePasswordAuthenticationToken(
                "chefesecao", "senha123", List.of(new SimpleGrantedAuthority("ROLE_CHEFE_SECAO")));

        List<GuiaAssinaturaResponse> listaMed = service.listarPendencias("med", authMed);
        assertEquals(1, listaMed.size());
        assertEquals("AM", listaMed.get(0).statusCodigo());

        List<GuiaAssinaturaResponse> listaSec = service.listarPendencias("sec", authSec);
        assertEquals(1, listaSec.size());
        assertEquals("AS", listaSec.get(0).statusCodigo());
    }

    @Test
    @DisplayName("Deve obter detalhes completos e PDF espelho da guia para conferência")
    void deveObterDetalhesEPdfDaGuia() {
        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setId(1L);
        guia.setNumeroGuia("202610700");
        guia.setBeneficiarioNome("Marcos V. Andrade");
        guia.setBeneficiarioPrecCp("34766899402");
        guia.setBeneficiarioEmail("marcos@email.com.br");
        guia.setOcsNome("Hospital Policlin S/A");
        guia.setNomeArquivoOriginal("guia_202610700.pdf");
        guia.setStatus(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);
        guia.setDataEnvio(LocalDateTime.now());

        when(guiaAssinaturaRepository.findById(1L)).thenReturn(Optional.of(guia));

        GuiaAssinaturaDetalheResponse detalhes = service.obterDetalhes(1L);
        assertEquals("202610700", detalhes.numeroGuia());
        assertEquals("60 dias a partir da assinatura do Chefe da Seção", detalhes.regraValidade());
        assertFalse(detalhes.procedimentos().isEmpty());

        ArquivoLoteDownload pdfDownload = service.obterArquivoPdf(1L);
        assertNotNull(pdfDownload.conteudo());
        assertTrue(pdfDownload.conteudo().length > 100);
        assertEquals("application/pdf", pdfDownload.tipo());
    }

    @Test
    @DisplayName("Deve bloquear assinatura quando checkbox não for confirmado ou canvas estiver vazio")
    void deveBloquearAssinaturaSemCheckboxOuSemCanvas() {
        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setId(1L);
        guia.setNumeroGuia("202610700");
        guia.setStatus(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);
        when(guiaAssinaturaRepository.findById(1L)).thenReturn(Optional.of(guia));

        Authentication authMed = new UsernamePasswordAuthenticationToken(
                "medicochefe", "senha123", List.of(new SimpleGrantedAuthority("ROLE_MEDICO_CHEFE")));

        DecisaoAssinaturaRequest reqSemCheck = new DecisaoAssinaturaRequest(
                DecisaoAssinatura.APROVAR, "med", false, ASSINATURA_PNG_BASE64, null);
        assertThrows(ValidationException.class, () -> service.processarDecisao(1L, reqSemCheck, authMed));

        DecisaoAssinaturaRequest reqSemCanvas = new DecisaoAssinaturaRequest(
                DecisaoAssinatura.APROVAR, "med", true, "   ", null);
        assertThrows(ValidationException.class, () -> service.processarDecisao(1L, reqSemCanvas, authMed));
    }

    @Test
    @DisplayName("Deve executar o fluxo completo de dupla assinatura com validade de 60 dias")
    void deveExecutarFluxoDuplaAssinaturaComCalculoDeValidade60Dias() {
        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setId(1L);
        guia.setNumeroGuia("202610700");
        guia.setBeneficiarioNome("Marcos V. Andrade");
        guia.setBeneficiarioPrecCp("34766899402");
        guia.setOcsNome("Hospital Policlin S/A");
        guia.setStatus(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);

        when(guiaAssinaturaRepository.findById(1L)).thenReturn(Optional.of(guia));
        when(fileStorageService.salvar(any(byte[].class), anyString())).thenReturn("uploads/guia_assinada.pdf");
        when(guiaAssinaturaRepository.save(any(GuiaAssinatura.class))).thenAnswer(inv -> inv.getArgument(0));

        Authentication authMed = new UsernamePasswordAuthenticationToken(
                "medicochefe", "senha123", List.of(new SimpleGrantedAuthority("ROLE_MEDICO_CHEFE")));
        Authentication authSec = new UsernamePasswordAuthenticationToken(
                "chefesecao", "senha123", List.of(new SimpleGrantedAuthority("ROLE_CHEFE_SECAO")));

        // Chefe da Seção não pode assinar antes do Médico Chefe
        DecisaoAssinaturaRequest reqSecPrecoce = new DecisaoAssinaturaRequest(
                DecisaoAssinatura.APROVAR, "sec", true, ASSINATURA_PNG_BASE64, null);
        assertThrows(UnauthorizedOperationException.class, () -> service.processarDecisao(1L, reqSecPrecoce, authSec));

        DecisaoAssinaturaRequest reqMed = new DecisaoAssinaturaRequest(
                DecisaoAssinatura.APROVAR, "med", true, ASSINATURA_PNG_BASE64, null);
        GuiaAssinaturaResponse resp1 = service.processarDecisao(1L, reqMed, authMed);

        assertEquals(StatusEsteiraAssinatura.AGUARDANDO_CHEFE_SECAO, resp1.status());
        assertEquals("AS", resp1.statusCodigo());
        assertNotNull(resp1.dataAssinaturaMedico());
        assertNull(resp1.dataValidade());

        DecisaoAssinaturaRequest reqSec = new DecisaoAssinaturaRequest(
                DecisaoAssinatura.APROVAR, "sec", true, ASSINATURA_PNG_BASE64, null);
        GuiaAssinaturaResponse resp2 = service.processarDecisao(1L, reqSec, authSec);

        assertEquals(StatusEsteiraAssinatura.AGUARDANDO_ENVIO_BENEFICIARIO, resp2.status());
        assertEquals("PE", resp2.statusCodigo());
        assertNotNull(resp2.dataAssinaturaSecao());
        assertEquals(LocalDate.now().plusDays(60), resp2.dataValidade());
    }

    @Test
    @DisplayName("Deve permitir rejeitar guia pendente mudando status para RJ")
    void devePermitirRejeitarGuiaPendente() {
        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setId(2L);
        guia.setNumeroGuia("202610701");
        guia.setBeneficiarioNome("Helena S. Couto");
        guia.setOcsNome("Clínica Imagem Sul");
        guia.setStatus(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);

        when(guiaAssinaturaRepository.findById(2L)).thenReturn(Optional.of(guia));
        when(guiaAssinaturaRepository.save(any(GuiaAssinatura.class))).thenAnswer(inv -> inv.getArgument(0));

        Authentication authMed = new UsernamePasswordAuthenticationToken(
                "medicochefe", "senha123", List.of(new SimpleGrantedAuthority("ROLE_MEDICO_CHEFE")));

        DecisaoAssinaturaRequest reqRejeitar = new DecisaoAssinaturaRequest(
                DecisaoAssinatura.REJEITAR, "med", false, null, "Procedimento divergente");

        GuiaAssinaturaResponse resp = service.processarDecisao(2L, reqRejeitar, authMed);
        assertEquals(StatusEsteiraAssinatura.REJEITADA, resp.status());
        assertEquals("RJ", resp.statusCodigo());
        assertEquals("Guia rejeitada.", resp.mensagem());
    }

    @Test
    @DisplayName("Deve enviar guia pronta (PE) por e-mail ao beneficiário e mover para enviadas (EN)")
    void deveEnviarEmailBeneficiarioEMoverParaEnviadas() {
        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setId(4L);
        guia.setNumeroGuia("202610703");
        guia.setBeneficiarioNome("Luana P. Azevedo");
        guia.setBeneficiarioPrecCp("34766901840");
        guia.setBeneficiarioEmail("luana.azevedo@email.com.br");
        guia.setOcsNome("Oftalmo Vale S/S Ltda");
        guia.setStatus(StatusEsteiraAssinatura.AGUARDANDO_ENVIO_BENEFICIARIO);
        guia.setDataAssinaturaSecao(LocalDateTime.now());
        guia.setDataValidade(LocalDate.now().plusDays(60));

        when(guiaAssinaturaRepository.findById(4L)).thenReturn(Optional.of(guia));
        when(emailService.enviarEmailGuiaAssinada(any(), any(), anyString()))
                .thenReturn(ResultadoEnvioEmail.sucesso("luana.azevedo@email.com.br"));
        when(guiaAssinaturaRepository.save(any(GuiaAssinatura.class))).thenAnswer(inv -> inv.getArgument(0));

        GuiaAssinaturaResponse resp = service.enviarEmailBeneficiario(4L);

        assertEquals(StatusEsteiraAssinatura.ENVIADA_BENEFICIARIO, resp.status());
        assertEquals("EN", resp.statusCodigo());
        assertEquals("Enviada ao beneficiário", resp.statusDescricao());
        assertNotNull(resp.dataEnvioEmail());
        assertEquals(StatusEmail.ENVIADO, resp.statusEmail());
        assertTrue(resp.mensagemPronta().contains("Guia nº 202610703"));
        assertTrue(resp.mensagemPronta().contains("Oftalmo Vale S/S Ltda"));
    }

    @Test
    @DisplayName("Deve bloquear rejeição fora da alçada da etapa atual")
    void deveBloquearRejeicaoForaDeAlcada() {
        GuiaAssinatura guiaAm = new GuiaAssinatura();
        guiaAm.setId(10L);
        guiaAm.setNumeroGuia("202610910");
        guiaAm.setStatus(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);

        GuiaAssinatura guiaAs = new GuiaAssinatura();
        guiaAs.setId(11L);
        guiaAs.setNumeroGuia("202610911");
        guiaAs.setStatus(StatusEsteiraAssinatura.AGUARDANDO_CHEFE_SECAO);

        when(guiaAssinaturaRepository.findById(10L)).thenReturn(Optional.of(guiaAm));
        when(guiaAssinaturaRepository.findById(11L)).thenReturn(Optional.of(guiaAs));

        Authentication authSec = new UsernamePasswordAuthenticationToken(
                "chefesecao", "senha123", List.of(new SimpleGrantedAuthority("ROLE_CHEFE_SECAO")));
        Authentication authMed = new UsernamePasswordAuthenticationToken(
                "medicochefe", "senha123", List.of(new SimpleGrantedAuthority("ROLE_MEDICO_CHEFE")));

        DecisaoAssinaturaRequest rejSec = new DecisaoAssinaturaRequest(
                DecisaoAssinatura.REJEITAR, "sec", false, null, "Fora da vez");
        assertThrows(UnauthorizedOperationException.class, () -> service.processarDecisao(10L, rejSec, authSec));

        DecisaoAssinaturaRequest rejMed = new DecisaoAssinaturaRequest(
                DecisaoAssinatura.REJEITAR, "med", false, null, "Fora da vez");
        assertThrows(UnauthorizedOperationException.class, () -> service.processarDecisao(11L, rejMed, authMed));
    }

    @Test
    @DisplayName("Deve rejeitar arquivo falso renomeado para .pdf e assinatura Base64 malformada")
    void deveRejeitarPdfFalsoEAssinaturaBase64Invalida() {
        MockMultipartFile falsoPdf = new MockMultipartFile(
                "arquivo", "falso.pdf", "application/pdf", "Isto nao eh um PDF real".getBytes());
        assertThrows(ArquivoInvalidoException.class, () -> service.enviarGuiaParaAssinatura(falsoPdf));

        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setId(12L);
        guia.setNumeroGuia("202610912");
        guia.setStatus(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);
        when(guiaAssinaturaRepository.findById(12L)).thenReturn(Optional.of(guia));

        Authentication authMed = new UsernamePasswordAuthenticationToken(
                "medicochefe", "senha123", List.of(new SimpleGrantedAuthority("ROLE_MEDICO_CHEFE")));

        DecisaoAssinaturaRequest reqXss = new DecisaoAssinaturaRequest(
                DecisaoAssinatura.APROVAR, "med", true, "data:image/png;base64,\" onerror=\"alert(1)", null);
        assertThrows(ValidationException.class, () -> service.processarDecisao(12L, reqXss, authMed));
    }

    @Test
    @DisplayName("Não deve avançar guia para ENVIADA_BENEFICIARIO quando o envio SMTP falhar")
    void naoDeveMarcarComoEnviadaQuandoSmtpFalhar() {
        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setId(13L);
        guia.setNumeroGuia("202610913");
        guia.setBeneficiarioNome("Cap. Renato");
        guia.setBeneficiarioEmail("renato@email.com.br");
        guia.setOcsNome("Hospital Policlin S/A");
        guia.setStatus(StatusEsteiraAssinatura.AGUARDANDO_ENVIO_BENEFICIARIO);
        guia.setDataValidade(LocalDate.now().plusDays(60));

        when(guiaAssinaturaRepository.findById(13L)).thenReturn(Optional.of(guia));
        when(emailService.enviarEmailGuiaAssinada(any(), any(), anyString()))
                .thenReturn(ResultadoEnvioEmail.falhaConexao("Falha SMTP timeout"));

        assertThrows(ValidationException.class, () -> service.enviarEmailBeneficiario(13L));
        assertEquals(StatusEsteiraAssinatura.AGUARDANDO_ENVIO_BENEFICIARIO, guia.getStatus());
        verify(guiaAssinaturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve extrair número da guia e nome do beneficiário sem hífen residual a partir do espelho PDF")
    void deveExtrairSemHifenResidualDoPdfEspelho() {
        GuiaAssinatura modelo = new GuiaAssinatura();
        modelo.setNumeroGuia("202610710");
        modelo.setBeneficiarioNome("Cap. Andre L. Cavalcante");
        modelo.setBeneficiarioPrecCp("34912345609");
        modelo.setBeneficiarioEmail("andre@email.com.br");
        modelo.setOcsNome("Hospital Policlin S/A");

        byte[] pdfGerado = pdfService.gerarPdfEspelhoGuia(modelo, pdfService.padraoProcedimentos());
        GuiaAssinaturaPdfService.DadosExtraidosPdf extraidos = pdfService.extrairDadosDoPdf(pdfGerado);

        assertEquals("202610710", extraidos.numeroGuia());
        assertEquals("Cap. Andre L. Cavalcante", extraidos.beneficiarioNome());
        assertEquals("34912345609", extraidos.beneficiarioPrecCp());
        assertEquals("Hospital Policlin S/A", extraidos.ocsNome());
    }

    @Test
    @DisplayName("Deve sanitizar barra em número de guia válido no disco e bloquear tentativa de Path Traversal")
    void deveSanitizarBarraEmNumeroGuiaEBloquearPathTraversal() throws Exception {
        byte[] pdfComBarra = gerarPdfGuiaValido("2026/10788", "Maj. Felipe Prado", "34123456789", "Hospital Policlin S/A");
        MockMultipartFile arqBarra = new MockMultipartFile("arquivo", "C:\\fakepath\\guia.pdf", "application/pdf", pdfComBarra);

        when(guiaAssinaturaRepository.existsByNumeroGuia("2026/10788")).thenReturn(false);
        when(fileStorageService.salvar(any(byte[].class), argThat(nome -> !nome.contains("/") && !nome.contains("\\") && nome.startsWith("guia_assinatura_2026_10788_"))))
                .thenReturn("uploads/guia_assinatura_2026_10788.pdf");
        when(guiaAssinaturaRepository.save(any(GuiaAssinatura.class))).thenAnswer(inv -> {
            GuiaAssinatura g = inv.getArgument(0);
            g.setId(55L);
            return g;
        });

        GuiaAssinaturaResponse resp = service.enviarGuiaParaAssinatura(arqBarra);
        assertEquals("2026/10788", resp.numeroGuia());
        assertEquals("guia.pdf", resp.nomeArquivoOriginal());

        byte[] pdfTraversal = gerarPdfGuiaValido("../../evil", "Teste", "34123456789", "Hospital Policlin S/A");
        MockMultipartFile arqTraversal = new MockMultipartFile("arquivo", "evil.pdf", "application/pdf", pdfTraversal);
        assertThrows(ArquivoInvalidoException.class, () -> service.enviarGuiaParaAssinatura(arqTraversal));
    }

    @Test
    @DisplayName("Deve rejeitar Base64 que não seja imagem real e bloquear médico solicitante comum")
    void deveRejeitarBase64NaoImagemEBloquearRoleMedicoComum() {
        GuiaAssinatura guia = new GuiaAssinatura();
        guia.setId(15L);
        guia.setNumeroGuia("202610915");
        guia.setStatus(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);
        when(guiaAssinaturaRepository.findById(15L)).thenReturn(Optional.of(guia));

        Authentication authMedChefe = new UsernamePasswordAuthenticationToken(
                "medicochefe", "senha123", List.of(new SimpleGrantedAuthority("ROLE_MEDICO_CHEFE")));
        DecisaoAssinaturaRequest reqTextoBase64 = new DecisaoAssinaturaRequest(
                DecisaoAssinatura.APROVAR, "med", true, "data:image/png;base64,dGVzdGU=", null);
        assertThrows(ValidationException.class, () -> service.processarDecisao(15L, reqTextoBase64, authMedChefe));

        Authentication authMedicoComum = new UsernamePasswordAuthenticationToken(
                "medico", "senha123", List.of(new SimpleGrantedAuthority("ROLE_MEDICO")));
        DecisaoAssinaturaRequest reqMedicoComum = new DecisaoAssinaturaRequest(
                DecisaoAssinatura.APROVAR, "med", true, ASSINATURA_PNG_BASE64, null);
        assertThrows(UnauthorizedOperationException.class, () -> service.processarDecisao(15L, reqMedicoComum, authMedicoComum));
    }
}
