package com.eagle.fusex.loader;

import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.repository.OcsRepository;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Component
@Order(3)
public class OcsCadastroLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OcsCadastroLoader.class);
    private static final String CSV_PATH_PADRAO = "data/ocs_cadastro.csv";

    private final OcsRepository ocsRepository;
    private final String csvPath;
    private final String filesystemFallback;

    @Autowired
    public OcsCadastroLoader(OcsRepository ocsRepository,
                             @Value("${fusex.import.ocs-cadastro-csv:" + CSV_PATH_PADRAO + "}") String csvPath) {
        this(ocsRepository, csvPath, "src/main/resources/" + CSV_PATH_PADRAO);
    }

    OcsCadastroLoader(OcsRepository ocsRepository, String csvPath, String filesystemFallback) {
        this.ocsRepository = ocsRepository;
        this.csvPath = csvPath;
        this.filesystemFallback = filesystemFallback;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        InputStream inputStream = abrirCsv();
        if (inputStream == null) {
            log.warn("AVISO: {} não encontrado — rode scripts/extrair_cadastro_ocs.py antes de subir a aplicação.", csvPath);
            return;
        }

        int atualizadas = 0;

        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setDelimiter(';')
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                Optional<Ocs> ocsExistente = ocsRepository.findByOcsNome(record.get("ocs_nome"));
                if (ocsExistente.isEmpty()) {
                    continue;
                }

                Ocs ocs = ocsExistente.get();
                if (ocs.getOcsInscricaoFederal() != null) {
                    continue;
                }

                ocs.setOcsInscricaoFederal(vazioParaNull(record.get("ocs_inscricao_federal")));
                ocs.setOcsEndereco(vazioParaNull(record.get("ocs_endereco")));
                ocs.setOcsEnderecoNumero(vazioParaNull(record.get("ocs_endereco_numero")));
                ocs.setOcsEnderecoBairro(vazioParaNull(record.get("ocs_endereco_bairro")));
                ocs.setOcsEnderecoCidade(vazioParaNull(record.get("ocs_endereco_cidade")));
                ocs.setOcsEnderecoUf(vazioParaNull(record.get("ocs_endereco_uf")));
                ocs.setOcsEnderecoCep(vazioParaNull(record.get("ocs_endereco_cep")));
                ocs.setOcsContatoTelefone(vazioParaNull(record.get("ocs_contato_telefone")));

                ocsRepository.save(ocs);
                atualizadas++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao importar cadastro de OCS", e);
        }

        log.info("{} OCS atualizadas com CNPJ/endereço/telefone!", atualizadas);
    }

    private String vazioParaNull(String valor) {
        return (valor == null || valor.isBlank()) ? null : valor;
    }

    private InputStream abrirCsv() throws IOException {
        ClassPathResource resource = new ClassPathResource(csvPath);
        if (resource.exists()) {
            return resource.getInputStream();
        }

        if (filesystemFallback != null) {
            Path fallback = Path.of(filesystemFallback);
            if (Files.exists(fallback)) {
                return new FileInputStream(fallback.toFile());
            }
        }

        return null;
    }
}
