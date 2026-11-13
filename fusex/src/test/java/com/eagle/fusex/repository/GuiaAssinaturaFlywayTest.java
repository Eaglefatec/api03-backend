package com.eagle.fusex.repository;

import com.eagle.fusex.model.entity.GuiaAssinatura;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.OcsProcedimento;
import com.eagle.fusex.model.entity.Medico;
import com.eagle.fusex.model.entity.Paciente;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.model.entity.SolicitacaoMedica;
import com.eagle.fusex.model.enums.StatusEsteiraAssinatura;
import org.flywaydb.core.Flyway;
import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.AvailableSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:flyway_v23_boot;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE",
        "spring.flyway.enabled=true",
        "spring.flyway.locations=classpath:db/migration",
        "spring.jpa.hibernate.ddl-auto=update"
})
class GuiaAssinaturaFlywayTest {

    @Autowired
    private Flyway flyway;

    @Autowired
    private GuiaAssinaturaRepository guiaAssinaturaRepository;

    @Test
    @DisplayName("Deve validar o mapeamento da tabela criada na migration V23 com a entidade GuiaAssinatura")
    void deveValidarSchemaEstritoDaMigrationV23ContraEntidadeJpa() {
        DataSource strictDs = DataSourceBuilder.create()
                .driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:flyway_v23_strict;MODE=MySQL;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE")
                .username("sa")
                .password("")
                .build();

        Flyway strictFlyway = Flyway.configure()
                .dataSource(strictDs)
                .locations("classpath:db/migration")
                .load();
        strictFlyway.migrate();

        assertNotNull(strictFlyway.info().current());
        assertEquals("25", strictFlyway.info().current().getVersion().getVersion());

        try (StandardServiceRegistry registry = new StandardServiceRegistryBuilder()
                .applySetting(AvailableSettings.DATASOURCE, strictDs)
                .applySetting(AvailableSettings.DIALECT, "org.hibernate.dialect.H2Dialect")
                .applySetting(AvailableSettings.HBM2DDL_AUTO, "validate")
                .applySetting(AvailableSettings.PHYSICAL_NAMING_STRATEGY,
                        "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy")
                .build()) {

            Metadata metadata = new MetadataSources(registry)
                    .addAnnotatedClass(GuiaAssinatura.class)
                    .addAnnotatedClass(Ocs.class)
                    .addAnnotatedClass(OcsProcedimento.class)
                    .addAnnotatedClass(Procedimento.class)
                    .addAnnotatedClass(SolicitacaoMedica.class)
                    .addAnnotatedClass(Medico.class)
                    .addAnnotatedClass(Paciente.class)
                    .buildMetadata();

            assertDoesNotThrow(() -> {
                try (SessionFactory sf = metadata.buildSessionFactory()) {
                    assertNotNull(sf);
                }
            });
        }
    }

    @Test
    @DisplayName("Deve subir a aplicação com a migration V23, carregar os dados iniciais e salvar nova guia")
    void deveSubirContextoComFlywayV23EPersistirGuia() {
        assertNotNull(flyway.info().current());
        assertEquals("25", flyway.info().current().getVersion().getVersion());

        assertTrue(guiaAssinaturaRepository.existsByNumeroGuia("202610700"));
        assertTrue(guiaAssinaturaRepository.existsByNumeroGuia("202610701"));
        assertTrue(guiaAssinaturaRepository.existsByNumeroGuia("202610702"));
        assertTrue(Files.exists(Path.of(System.getProperty("java.io.tmpdir"), "fusex-test-uploads", "exemplo_nova_guia_202610710.pdf")));

        GuiaAssinatura nova = new GuiaAssinatura();
        nova.setNumeroGuia("202699001");
        nova.setBeneficiarioNome("1º Ten Lucas M. Farias");
        nova.setBeneficiarioPrecCp("34111222333");
        nova.setBeneficiarioEmail("lucas.farias@email.com.br");
        nova.setOcsNome("Hospital Policlin S/A");
        nova.setNomeArquivoOriginal("guia_202699001.pdf");
        nova.setCaminhoArquivoOriginal("uploads/guia_202699001.pdf");
        nova.setTamanhoBytes(10240L);
        nova.setStatus(StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE);
        nova.setDataEnvio(LocalDateTime.now());

        GuiaAssinatura salva = guiaAssinaturaRepository.save(nova);
        assertNotNull(salva.getId());
        assertTrue(guiaAssinaturaRepository.existsByNumeroGuia("202699001"));
    }
}
