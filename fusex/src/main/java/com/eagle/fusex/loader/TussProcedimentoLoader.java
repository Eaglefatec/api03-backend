package com.eagle.fusex.loader;

import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.model.enums.OrigemProcedimento;
import com.eagle.fusex.repository.ProcedimentoRepository;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

@Component
@Order(1)
public class TussProcedimentoLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(TussProcedimentoLoader.class);
    private static final String CSV_PATH_PADRAO = "data/tuss_procedimentos.csv";

    private final ProcedimentoRepository procedimentoRepository;
    private final String csvPath;
    private final String filesystemFallback;

    @Autowired
    public TussProcedimentoLoader(ProcedimentoRepository procedimentoRepository,
                                  @Value("${fusex.import.tuss-csv:" + CSV_PATH_PADRAO + "}") String csvPath) {
        this(procedimentoRepository, csvPath, "src/main/resources/" + CSV_PATH_PADRAO);
    }

    TussProcedimentoLoader(ProcedimentoRepository procedimentoRepository, String csvPath, String filesystemFallback) {
        this.procedimentoRepository = procedimentoRepository;
        this.csvPath = csvPath;
        this.filesystemFallback = filesystemFallback;
    }

    @Override
    public void run(String... args) throws Exception {
        if (procedimentoRepository.count() > 0) {
            return;
        }

        InputStream inputStream = abrirCsv();
        if (inputStream == null) {
            log.warn("AVISO: {} não encontrado — rode scripts/extrair_planilhas.py antes de subir a aplicação.", csvPath);
            return;
        }

        List<Procedimento> procedimentos = new ArrayList<>();

        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setDelimiter(';')
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                Procedimento procedimento = new Procedimento(
                        record.get("proc_codigo_dgp"),
                        record.get("proc_descricao"),
                        1
                );
                procedimento.setProcOrigem(OrigemProcedimento.TUSS);
                procedimentos.add(procedimento);
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao importar procedimentos TUSS", e);
        }

        procedimentoRepository.saveAll(procedimentos);
        log.info("{} procedimentos TUSS importados com sucesso!", procedimentos.size());
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
