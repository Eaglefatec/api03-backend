package com.eagle.fusex.loader;

import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.OcsProcedimento;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.model.enums.Especialidade;
import com.eagle.fusex.model.enums.OrigemProcedimento;
import com.eagle.fusex.model.enums.TipoOcs;
import com.eagle.fusex.repository.OcsProcedimentoRepository;
import com.eagle.fusex.repository.OcsRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Component
@Order(2)
public class OcsPrecoLoader implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(OcsPrecoLoader.class);
    private static final String CSV_PATH_PADRAO = "data/ocs_precos.csv";

    private final OcsRepository ocsRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final OcsProcedimentoRepository ocsProcedimentoRepository;
    private final String csvPath;
    private final String filesystemFallback;

    @Autowired
    public OcsPrecoLoader(OcsRepository ocsRepository, ProcedimentoRepository procedimentoRepository,
                          OcsProcedimentoRepository ocsProcedimentoRepository,
                          @Value("${fusex.import.ocs-precos-csv:" + CSV_PATH_PADRAO + "}") String csvPath) {
        this(ocsRepository, procedimentoRepository, ocsProcedimentoRepository, csvPath,
                "src/main/resources/" + CSV_PATH_PADRAO);
    }

    OcsPrecoLoader(OcsRepository ocsRepository, ProcedimentoRepository procedimentoRepository,
                   OcsProcedimentoRepository ocsProcedimentoRepository, String csvPath, String filesystemFallback) {
        this.ocsRepository = ocsRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.ocsProcedimentoRepository = ocsProcedimentoRepository;
        this.csvPath = csvPath;
        this.filesystemFallback = filesystemFallback;
    }

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        if (ocsProcedimentoRepository.count() > 0) {
            return;
        }

        InputStream inputStream = abrirCsv();
        if (inputStream == null) {
            log.warn("AVISO: {} não encontrado — rode scripts/extrair_planilhas.py antes de subir a aplicação.", csvPath);
            return;
        }

        Map<String, Ocs> ocsCache = new HashMap<>();
        int totalPrecos = 0;

        try (InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.DEFAULT.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setDelimiter(';')
                     .build()
                     .parse(reader)) {

            for (CSVRecord record : parser) {
                String ocsNome = record.get("ocs_nome");
                Ocs ocs = ocsCache.computeIfAbsent(ocsNome, nome -> obterOuCriarOcs(record));

                String procCodigoDgp = record.get("proc_codigo_dgp");
                Procedimento procedimento = procedimentoRepository.findById(procCodigoDgp)
                        .orElseGet(() -> criarProcedimentoLocal(record));

                OcsProcedimento ocsProcedimento = new OcsProcedimento();
                ocsProcedimento.setOcs(ocs);
                ocsProcedimento.setProcedimento(procedimento);
                ocsProcedimento.setValor(new BigDecimal(record.get("valor")));
                ocsProcedimento.setTabelaReferencia(vazioParaNull(record.get("tabela_referencia")));
                ocsProcedimento.setDescricaoGrupo(vazioParaNull(record.get("descricao_grupo")));

                ocsProcedimentoRepository.save(ocsProcedimento);
                totalPrecos++;
            }
        } catch (IOException e) {
            throw new RuntimeException("Erro ao importar preços por OCS", e);
        }

        log.info("{} OCS e {} preços de procedimento importados com sucesso!", ocsCache.size(), totalPrecos);
    }

    private Ocs obterOuCriarOcs(CSVRecord record) {
        String nome = record.get("ocs_nome");
        return ocsRepository.findByOcsNome(nome).orElseGet(() -> {
            Ocs ocs = new Ocs();
            ocs.setOcsNome(nome);
            ocs.setOcsTipo("PSA".equalsIgnoreCase(record.get("ocs_tipo")) ? TipoOcs.PSA : TipoOcs.OCS);
            ocs.setOcsContratoNumero(vazioParaNull(record.get("ocs_contrato_numero")));
            ocs.setOcsInicioVigencia(parseData(record.get("ocs_inicio_vigencia")));
            ocs.setOcsTerminoVigencia(parseData(record.get("ocs_termino_vigencia")));
            ocs.setEspecialidades(parseEspecialidades(record.get("especialidades")));
            return ocsRepository.save(ocs);
        });
    }

    private Procedimento criarProcedimentoLocal(CSVRecord record) {
        Procedimento procedimento = new Procedimento(
                record.get("proc_codigo_dgp"),
                record.get("proc_descricao"),
                1
        );
        String origem = record.get("proc_origem");
        procedimento.setProcOrigem("OCS_LOCAL".equals(origem) ? OrigemProcedimento.OCS_LOCAL : OrigemProcedimento.TUSS);
        return procedimentoRepository.save(procedimento);
    }

    private LocalDate parseData(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return LocalDate.parse(valor);
    }

    private Set<Especialidade> parseEspecialidades(String valor) {
        Set<Especialidade> especialidades = new HashSet<>();
        if (valor == null || valor.isBlank()) {
            especialidades.add(Especialidade.OUTROS);
            return especialidades;
        }
        for (String parte : valor.split(",")) {
            try {
                especialidades.add(Especialidade.valueOf(parte.trim()));
            } catch (IllegalArgumentException e) {
                especialidades.add(Especialidade.OUTROS);
            }
        }
        return especialidades;
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
