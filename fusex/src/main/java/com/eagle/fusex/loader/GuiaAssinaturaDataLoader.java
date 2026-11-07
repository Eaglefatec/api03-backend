package com.eagle.fusex.loader;

import com.eagle.fusex.model.entity.GuiaAssinatura;
import com.eagle.fusex.model.enums.StatusEmail;
import com.eagle.fusex.model.enums.StatusEsteiraAssinatura;
import com.eagle.fusex.repository.GuiaAssinaturaRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.service.GuiaAssinaturaPdfService;
import com.eagle.fusex.storage.FileStorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@Order(10)
public class GuiaAssinaturaDataLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(GuiaAssinaturaDataLoader.class);

    private final GuiaAssinaturaRepository guiaAssinaturaRepository;
    private final OcsRepository ocsRepository;
    private final GuiaAssinaturaPdfService pdfService;
    private final FileStorageService fileStorageService;

    public GuiaAssinaturaDataLoader(GuiaAssinaturaRepository guiaAssinaturaRepository,
                                    OcsRepository ocsRepository,
                                    GuiaAssinaturaPdfService pdfService,
                                    FileStorageService fileStorageService) {
        this.guiaAssinaturaRepository = guiaAssinaturaRepository;
        this.ocsRepository = ocsRepository;
        this.pdfService = pdfService;
        this.fileStorageService = fileStorageService;
    }

    @Override
    public void run(String... args) {
        if (guiaAssinaturaRepository.count() > 0) {
            return;
        }

        try {
            GuiaAssinatura g1 = criarGuiaInicial(
                    "202610700",
                    "Helena M. Prado",
                    "34596112300",
                    "Plani Diagnósticos Médicos Ltda",
                    "helena.prado@email.com.br",
                    LocalDateTime.of(2026, 10, 30, 9, 20),
                    StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE,
                    null,
                    null,
                    null
            );

            GuiaAssinatura g2 = criarGuiaInicial(
                    "202610701",
                    "Rogério A. Tavares",
                    "34410998801",
                    "Clínica Ortopédica Santa Lúcia",
                    "rogerio.tavares@email.com.br",
                    LocalDateTime.of(2026, 10, 30, 10, 45),
                    StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE,
                    null,
                    null,
                    null
            );

            GuiaAssinatura g3 = criarGuiaInicial(
                    "202610702",
                    "Marta S. Lacerda",
                    "35021877402",
                    "Plani Diagnósticos Médicos Ltda",
                    "marta.lacerda@email.com.br",
                    LocalDateTime.of(2026, 10, 29, 8, 50),
                    StatusEsteiraAssinatura.AGUARDANDO_CHEFE_SECAO,
                    LocalDateTime.of(2026, 10, 29, 9, 12),
                    null,
                    null
            );

            GuiaAssinatura g4 = criarGuiaInicial(
                    "202610703",
                    "Paulo C. Nogueira",
                    "34877120503",
                    "Fisio Vip Clin Serv. Fisioter. Ltda",
                    "paulo.nogueira@email.com.br",
                    LocalDateTime.of(2026, 10, 27, 9, 30),
                    StatusEsteiraAssinatura.AGUARDANDO_ENVIO_BENEFICIARIO,
                    LocalDateTime.of(2026, 10, 27, 10, 5),
                    LocalDateTime.of(2026, 10, 27, 14, 40),
                    null
            );

            GuiaAssinatura g5 = criarGuiaInicial(
                    "202610704",
                    "Sônia B. Amaral",
                    "35190442201",
                    "Fisio Vip Clin Serv. Fisioter. Ltda",
                    "sonia.amaral@email.com.br",
                    LocalDateTime.of(2026, 10, 28, 8, 10),
                    StatusEsteiraAssinatura.ENVIADA_BENEFICIARIO,
                    LocalDateTime.of(2026, 10, 28, 8, 50),
                    LocalDateTime.of(2026, 10, 28, 11, 15),
                    LocalDateTime.of(2026, 10, 28, 14, 10)
            );

            guiaAssinaturaRepository.saveAll(List.of(g1, g2, g3, g4, g5));

            // Deixa um PDF pronto na pasta uploads para facilitar testes de envio
            GuiaAssinatura modeloUploadTeste = new GuiaAssinatura();
            modeloUploadTeste.setNumeroGuia("202610710");
            modeloUploadTeste.setBeneficiarioNome("Cap. André L. Cavalcante");
            modeloUploadTeste.setBeneficiarioPrecCp("34912345609");
            modeloUploadTeste.setBeneficiarioEmail("andre.cavalcante@email.com.br");
            modeloUploadTeste.setOcsNome("Hospital Policlin S/A");
            modeloUploadTeste.setStatus(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);
            byte[] pdfExemploBytes = pdfService.gerarPdfEspelhoGuia(modeloUploadTeste, pdfService.padraoProcedimentos());
            fileStorageService.salvar(pdfExemploBytes, "exemplo_nova_guia_202610710.pdf");

            log.info("Esteira de assinaturas inicializada com 5 guias demonstrativas (202610700 a 202610704) e PDF de teste em uploads/exemplo_nova_guia_202610710.pdf.");
        } catch (Exception e) {
            log.warn("Não foi possível carregar dados iniciais de guia_assinatura: {}", e.getMessage());
        }
    }

    private GuiaAssinatura criarGuiaInicial(String numeroGuia,
                                            String beneficiario,
                                            String precCp,
                                            String ocsNome,
                                            String email,
                                            LocalDateTime dataEnvio,
                                            StatusEsteiraAssinatura status,
                                            LocalDateTime dataMed,
                                            LocalDateTime dataSec,
                                            LocalDateTime dataEmail) {
        GuiaAssinatura g = new GuiaAssinatura();
        g.setNumeroGuia(numeroGuia);
        g.setBeneficiarioNome(beneficiario);
        g.setBeneficiarioPrecCp(precCp);
        g.setBeneficiarioEmail(email);
        g.setOcsNome(ocsNome);
        ocsRepository.findByOcsNome(ocsNome).ifPresent(g::setOcs);
        g.setProcedimentosResumo(pdfService.serializarProcedimentos(pdfService.padraoProcedimentos()));
        g.setNomeArquivoOriginal("guia_" + numeroGuia + ".pdf");
        g.setStatus(status);
        g.setDataEnvio(dataEnvio);
        g.setCreatedAt(dataEnvio);

        if (dataMed != null) {
            g.setMedicoChefeUsuario("medicochefe");
            g.setDataAssinaturaMedico(dataMed);
            g.setAssinaturaMedicoBase64("ASSINADO_MEDICO_CHEFE");
        }
        if (dataSec != null) {
            g.setChefeSecaoUsuario("chefesecao");
            g.setDataAssinaturaSecao(dataSec);
            g.setAssinaturaSecaoBase64("ASSINADO_CHEFE_SECAO");
            LocalDate validade = dataSec.toLocalDate().plusDays(60);
            g.setDataValidade(validade);
        }
        if (dataEmail != null) {
            g.setDataEnvioEmail(dataEmail);
            g.setStatusEmail(StatusEmail.SIMULADO);
        }

        byte[] pdfBytes = pdfService.gerarPdfEspelhoGuia(g, pdfService.padraoProcedimentos());
        String caminho = fileStorageService.salvar(pdfBytes, "guia_" + numeroGuia + ".pdf");
        g.setCaminhoArquivoOriginal(caminho);
        if (dataMed != null || dataSec != null) {
            g.setCaminhoArquivoAssinado(caminho);
        }
        g.setTamanhoBytes((long) pdfBytes.length);
        return g;
    }
}

