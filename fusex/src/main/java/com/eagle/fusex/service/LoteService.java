package com.eagle.fusex.service;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.exception.OcsInvalidoException;
import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.exception.ValidationException;
import com.eagle.fusex.model.dto.response.LoteResponse;
import com.eagle.fusex.model.entity.Guia;
import com.eagle.fusex.model.entity.GuiaItem;
import com.eagle.fusex.model.entity.Lote;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.OcsProcedimento;
import com.eagle.fusex.model.enums.StatusGuia;
import com.eagle.fusex.repository.GuiaItemRepository;
import com.eagle.fusex.repository.GuiaRepository;
import com.eagle.fusex.repository.LoteRepository;
import com.eagle.fusex.repository.OcsProcedimentoRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.security.SecurityUtils;
import com.eagle.fusex.storage.FileStorageService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class LoteService {

    private static final Logger log = LoggerFactory.getLogger(LoteService.class);

    private final LoteRepository loteRepository;
    private final GuiaRepository guiaRepository;
    private final GuiaItemRepository guiaItemRepository;
    private final OcsRepository ocsRepository;
    private final FileStorageService fileStorageService;
    private final OcsProcedimentoRepository ocsProcedimentoRepository;

    public LoteService(LoteRepository loteRepository, GuiaRepository guiaRepository,
                       GuiaItemRepository guiaItemRepository, OcsRepository ocsRepository,
                       FileStorageService fileStorageService) {
        this(loteRepository, guiaRepository, guiaItemRepository, ocsRepository, fileStorageService, null);
    }

    @Autowired
    public LoteService(LoteRepository loteRepository, GuiaRepository guiaRepository,
                       GuiaItemRepository guiaItemRepository, OcsRepository ocsRepository,
                       FileStorageService fileStorageService,
                       OcsProcedimentoRepository ocsProcedimentoRepository) {
        this.loteRepository = loteRepository;
        this.guiaRepository = guiaRepository;
        this.guiaItemRepository = guiaItemRepository;
        this.ocsRepository = ocsRepository;
        this.fileStorageService = fileStorageService;
        this.ocsProcedimentoRepository = ocsProcedimentoRepository;
    }

    @Transactional
    public LoteResponse gerarLote(List<Long> guiaIds, MultipartFile fatura, MultipartFile espelho) {
        Ocs ocs = buscarOcsLogada();

        if (guiaIds == null || guiaIds.isEmpty()) {
            throw new ValidationException("Selecione ao menos uma guia para gerar o lote");
        }

        List<Long> idsUnicos = new ArrayList<>(new LinkedHashSet<>(guiaIds));
        List<Guia> guias = guiaRepository.findAllById(idsUnicos);
        if (guias.size() != idsUnicos.size()) {
            throw new ResourceNotFoundException("Uma ou mais guias informadas não foram encontradas");
        }
        for (Guia guia : guias) {
            if (!guia.getOcs().getOcsId().equals(ocs.getOcsId())) {
                throw new ValidationException("A guia " + guia.getNumeroGuia() + " não pertence a esta OCS");
            }
            if (guia.getStatus() != StatusGuia.SEM_LOTE) {
                throw new ValidationException("A guia " + guia.getNumeroGuia() + " já está em um lote");
            }
        }

        validarArquivoFatura(fatura);
        validarArquivoEspelho(espelho);

        LocalDate hoje = LocalDate.now();
        int competenciaAno = hoje.getYear();
        int competenciaMes = hoje.getMonthValue();
        int numeroLote = (int) loteRepository.countByOcs_OcsIdAndCompetenciaAnoAndCompetenciaMes(
                ocs.getOcsId(), competenciaAno, competenciaMes) + 1;

        String faturaCaminho = fileStorageService.salvar(fatura, UUID.randomUUID() + "." + extrairExtensao(fatura));
        String espelhoCaminho = fileStorageService.salvar(espelho, UUID.randomUUID() + "." + extrairExtensao(espelho));

        Lote lote = new Lote();
        lote.setOcs(ocs);
        lote.setNumeroLote(numeroLote);
        lote.setCompetenciaAno(competenciaAno);
        lote.setCompetenciaMes(competenciaMes);
        lote.setDataGeracao(LocalDateTime.now());
        lote.setFaturaNomeArquivo(fatura.getOriginalFilename());
        lote.setFaturaCaminhoArquivo(faturaCaminho);
        lote.setFaturaTipoArquivo(fatura.getContentType());
        lote.setEspelhoNomeArquivo(espelho.getOriginalFilename());
        lote.setEspelhoCaminhoArquivo(espelhoCaminho);
        lote.setEspelhoTipoArquivo(espelho.getContentType());

        Lote salvo = loteRepository.save(lote);

        Map<String, Guia> guiasPorNumero = guias.stream()
                .collect(Collectors.toMap(Guia::getNumeroGuia, g -> g, (g1, g2) -> g1));

        processarEspelhoImportado(espelho, guiasPorNumero);

        guias.forEach(guia -> {
            guia.setStatus(StatusGuia.EM_LOTE);
            guia.setLote(salvo);
        });
        guiaRepository.saveAll(guias);

        log.info("Lote {} gerado pela OCS {} com {} guias", salvo.getNumeroLote(), ocs.getOcsNome(), guias.size());

        return toResponse(salvo);
    }

    @Transactional(readOnly = true)
    public List<LoteResponse> listarLotes() {
        Ocs ocs = buscarOcsLogada();
        return loteRepository.findByOcs_LoginUsuarioAndFechamentoIsNullOrderByDataGeracaoDesc(ocs.getLoginUsuario()).stream()
                .map(this::toResponse)
                .toList();
    }

    private Ocs buscarOcsLogada() {
        String loginUsuario = SecurityUtils.getAuthenticatedUsername()
                .orElseThrow(() -> new OcsInvalidoException("Usuário não autenticado"));
        return ocsRepository.findByLoginUsuario(loginUsuario)
                .orElseThrow(() -> new OcsInvalidoException("Nenhuma OCS associada a este usuário"));
    }

    private void validarArquivoFatura(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Anexe a fatura em formato PDF");
        }
        String nome = arquivo.getOriginalFilename();
        String nomeMinusculo = nome != null ? nome.toLowerCase() : "";
        if (!nomeMinusculo.endsWith(".pdf")) {
            throw new ArquivoInvalidoException("Anexe a fatura em formato PDF");
        }
        validarTamanho(arquivo);
    }

    private void validarArquivoEspelho(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Anexe o espelho em PDF, XLSX ou CSV");
        }
        String nome = arquivo.getOriginalFilename();
        String nomeMinusculo = nome != null ? nome.toLowerCase() : "";
        boolean extensaoValida = nomeMinusculo.endsWith(".pdf")
                || nomeMinusculo.endsWith(".xlsx")
                || nomeMinusculo.endsWith(".csv");
        if (!extensaoValida) {
            throw new ArquivoInvalidoException("Anexe o espelho em PDF, XLSX ou CSV");
        }
        validarTamanho(arquivo);
    }

    private void validarTamanho(MultipartFile arquivo) {
        if (arquivo.getSize() > 10L * 1024 * 1024) {
            throw new ArquivoInvalidoException("O arquivo deve ter até 10 MB");
        }
    }

    private String extrairExtensao(MultipartFile arquivo) {
        String nome = arquivo.getOriginalFilename();
        if (nome == null || !nome.contains(".")) {
            return "bin";
        }
        return nome.substring(nome.lastIndexOf(".") + 1).toLowerCase();
    }

    private void processarEspelhoImportado(MultipartFile espelho, Map<String, Guia> guiasPorNumero) {
        String nome = espelho.getOriginalFilename();
        if (nome == null) return;

        String nomeMinusculo = nome.toLowerCase();

        try {
            if (nomeMinusculo.endsWith(".xlsx")) {
                processarExcel(espelho.getInputStream(), guiasPorNumero);
            } else if (nomeMinusculo.endsWith(".csv")) {
                processarCsv(espelho.getInputStream(), guiasPorNumero);
            }
        } catch (Exception e) {
            log.error("Erro ao processar o conteúdo do espelho importado: {}", e.getMessage(), e);
            throw new ArquivoInvalidoException("Erro ao ler os dados do espelho: " + e.getMessage());
        }
    }

    private void processarExcel(InputStream is, Map<String, Guia> guiasPorNumero) throws Exception {
        List<GuiaItem> itensParaSalvar = new ArrayList<>();
        List<Guia> guiasParaAtualizar = new ArrayList<>();
        Set<Guia> guiasSemValorNaGuia = new HashSet<>();
        Map<String, Optional<BigDecimal>> cachePrecos = new HashMap<>();

        try (Workbook workbook = WorkbookFactory.create(is)) {
            if (workbook.getNumberOfSheets() == 0) {
                return;
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            log.info("Total de linhas no Excel do espelho: {}", sheet.getLastRowNum());

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue;

                // Ordem das colunas na planilha do espelho: Nº da Guia, Beneficiário, Prec CP, Código TUSS, Descrição, Tipo, Quantidade e Valor Unitário
                String numeroGuia = getCellVal(row.getCell(0), formatter, evaluator);
                String beneficiarioNome = getCellVal(row.getCell(1), formatter, evaluator);
                String precCp = getCellVal(row.getCell(2), formatter, evaluator);
                String codigoTuss = getCellVal(row.getCell(3), formatter, evaluator);
                String descricao = getCellVal(row.getCell(4), formatter, evaluator);
                String tipo = getCellVal(row.getCell(5), formatter, evaluator);

                int quantidade = parseQuantidade(getCellVal(row.getCell(6), formatter, evaluator), row.getCell(6));
                BigDecimal valorUnitario = parseValorCelula(row.getCell(7), formatter, evaluator);

                if (!numeroGuia.isEmpty() && guiasPorNumero.containsKey(numeroGuia)) {
                    Guia guia = guiasPorNumero.get(numeroGuia);
                    registrarItemNaGuia(guia, beneficiarioNome, precCp, codigoTuss,
                            descricao.isEmpty() ? "Item sem descrição" : descricao,
                            tipo.isEmpty() ? "Procedimento" : tipo,
                            quantidade > 0 ? quantidade : 1, valorUnitario,
                            guiasParaAtualizar, itensParaSalvar, guiasSemValorNaGuia, cachePrecos);
                } else if (!numeroGuia.isEmpty()) {
                    log.warn("Linha do Excel com Guia '{}' ignorada ou não encontrada nas guias selecionadas.", numeroGuia);
                }
            }
        }

        if (!itensParaSalvar.isEmpty()) {
            guiaItemRepository.saveAll(itensParaSalvar);
            log.info("{} itens do espelho Excel salvos com sucesso.", itensParaSalvar.size());
        }

        if (!guiasParaAtualizar.isEmpty()) {
            guiaRepository.saveAll(guiasParaAtualizar);
            log.info("{} guias atualizadas com os dados do espelho Excel.", guiasParaAtualizar.size());
        }
    }

    private void processarCsv(InputStream is, Map<String, Guia> guiasPorNumero) throws Exception {
        List<GuiaItem> itensParaSalvar = new ArrayList<>();
        List<Guia> guiasParaAtualizar = new ArrayList<>();
        Set<Guia> guiasSemValorNaGuia = new HashSet<>();
        Map<String, Optional<BigDecimal>> cachePrecos = new HashMap<>();

        String texto = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        if (texto.startsWith("\uFEFF")) {
            texto = texto.substring(1);
        }
        char delimitador = detectarDelimitadorCsv(texto);

        try (Reader reader = new StringReader(texto)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.builder()
                    .setDelimiter(delimitador)
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .setIgnoreEmptyLines(true)
                    .setTrim(true)
                    .build()
                    .parse(reader);
            for (CSVRecord record : records) {
                if (record.size() < 8) continue;

                String numeroGuia = record.get(0) != null ? record.get(0).trim() : "";
                String beneficiarioNome = record.get(1) != null ? record.get(1).trim() : "";
                String precCp = record.get(2) != null ? record.get(2).trim() : "";
                String codigoTuss = record.get(3) != null ? record.get(3).trim() : "";
                String descricao = record.get(4) != null ? record.get(4).trim() : "";
                String tipo = record.get(5) != null ? record.get(5).trim() : "";

                int quantidade = parseQuantidade(record.get(6), null);
                BigDecimal valorUnitario = parseValorMonetario(record.get(7));

                if (!numeroGuia.isEmpty() && guiasPorNumero.containsKey(numeroGuia)) {
                    Guia guia = guiasPorNumero.get(numeroGuia);
                    registrarItemNaGuia(guia, beneficiarioNome, precCp, codigoTuss,
                            descricao.isBlank() ? "Item sem descrição" : descricao,
                            tipo.isBlank() ? "Procedimento" : tipo,
                            quantidade > 0 ? quantidade : 1, valorUnitario,
                            guiasParaAtualizar, itensParaSalvar, guiasSemValorNaGuia, cachePrecos);
                }
            }
        }

        if (!itensParaSalvar.isEmpty()) {
            guiaItemRepository.saveAll(itensParaSalvar);
            log.info("{} itens do espelho CSV salvos com sucesso.", itensParaSalvar.size());
        }

        if (!guiasParaAtualizar.isEmpty()) {
            guiaRepository.saveAll(guiasParaAtualizar);
            log.info("{} guias atualizadas com os dados do espelho CSV.", guiasParaAtualizar.size());
        }
    }

    private char detectarDelimitadorCsv(String texto) {
        int fimPrimeiraLinha = texto.indexOf('\n');
        String primeiraLinha = fimPrimeiraLinha >= 0 ? texto.substring(0, fimPrimeiraLinha) : texto;
        long pontoVirgula = primeiraLinha.chars().filter(c -> c == ';').count();
        long virgula = primeiraLinha.chars().filter(c -> c == ',').count();
        return pontoVirgula > virgula ? ';' : ',';
    }

    private int parseQuantidade(String raw, Cell cell) {
        if (cell != null && cell.getCellType() == CellType.NUMERIC) {
            int q = (int) Math.round(cell.getNumericCellValue());
            return q > 0 ? q : 1;
        }
        if (raw == null || raw.isBlank()) {
            return 1;
        }
        try {
            String limpo = raw.trim().replace(",", ".");
            int q = (int) Math.round(Double.parseDouble(limpo));
            return q > 0 ? q : 1;
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    private BigDecimal parseValorCelula(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) {
            return BigDecimal.ZERO;
        }
        if (cell.getCellType() == CellType.NUMERIC) {
            BigDecimal val = BigDecimal.valueOf(cell.getNumericCellValue());
            if (val.signum() < 0) {
                throw new ArquivoInvalidoException("Valor unitário não pode ser negativo no espelho.");
            }
            return val;
        }
        return parseValorMonetario(getCellVal(cell, formatter, evaluator));
    }

    private BigDecimal parseValorMonetario(String raw) {
        if (raw == null || raw.isBlank()) {
            return BigDecimal.ZERO;
        }
        String limpo = raw.replace("R$", "").replace("\u00A0", "").trim();
        if (limpo.isEmpty()) {
            return BigDecimal.ZERO;
        }
        if (limpo.contains(",") && limpo.contains(".")) {
            int lastComma = limpo.lastIndexOf(',');
            int lastDot = limpo.lastIndexOf('.');
            if (lastComma > lastDot) {
                limpo = limpo.replace(".", "").replace(",", ".");
            } else {
                limpo = limpo.replace(",", "");
            }
        } else if (limpo.contains(",")) {
            limpo = limpo.replace(",", ".");
        }
        try {
            BigDecimal val = new BigDecimal(limpo);
            if (val.signum() < 0) {
                throw new ArquivoInvalidoException("Valor unitário não pode ser negativo no espelho.");
            }
            return val;
        } catch (NumberFormatException e) {
            return BigDecimal.ZERO;
        }
    }

    private void registrarItemNaGuia(Guia guia, String beneficiarioNome, String precCp, String codigoTuss,
                                     String descricao, String tipo, int quantidade, BigDecimal valorUnitario,
                                     List<Guia> guiasParaAtualizar, List<GuiaItem> itensParaSalvar,
                                     Set<Guia> guiasSemValorNaGuia,
                                     Map<String, Optional<BigDecimal>> cachePrecos) {
        if (!guiasParaAtualizar.contains(guia)) {
            guia.setValorFaturado(BigDecimal.ZERO);
            if (guia.getValorContratado() == null || guia.getValorContratado().signum() <= 0) {
                guia.setValorContratado(BigDecimal.ZERO);
                guiasSemValorNaGuia.add(guia);
            }
            guiasParaAtualizar.add(guia);
        }

        if (beneficiarioNome != null && !beneficiarioNome.isBlank()) guia.setBeneficiarioNome(beneficiarioNome);
        if (precCp != null && !precCp.isBlank()) guia.setBeneficiarioPrecCp(precCp);
        if (codigoTuss != null && !codigoTuss.isBlank()) guia.setCodigoTuss(codigoTuss);

        BigDecimal qtd = BigDecimal.valueOf(quantidade);
        BigDecimal valorTotalItem = valorUnitario.multiply(qtd);
        guia.setValorFaturado(guia.getValorFaturado().add(valorTotalItem));

        if (guiasSemValorNaGuia.contains(guia)
                && ocsProcedimentoRepository != null && codigoTuss != null && !codigoTuss.isBlank()
                && guia.getOcs() != null && guia.getOcs().getOcsId() != null) {
            Long ocsId = guia.getOcs().getOcsId();
            String chave = ocsId + ":" + codigoTuss;
            Optional<BigDecimal> valorOpt = cachePrecos.computeIfAbsent(chave, k ->
                    ocsProcedimentoRepository
                            .findByOcs_OcsIdAndProcedimento_ProcCodigoDgp(ocsId, codigoTuss)
                            .map(OcsProcedimento::getValor));
            valorOpt.ifPresent(valorTabela -> guia.setValorContratado(
                    guia.getValorContratado().add(valorTabela.multiply(qtd))));
        }

        GuiaItem item = new GuiaItem();
        item.setGuiaId(guia.getId());
        item.setCodigoTuss(codigoTuss != null ? codigoTuss : "");
        item.setDescricaoItem(descricao);
        item.setTipo(tipo);
        item.setQuantidade(quantidade);
        item.setValorUnitario(valorUnitario);
        item.setDataItem(LocalDate.now());

        itensParaSalvar.add(item);
    }

    private String getCellVal(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        if (cell == null) return "";
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        }
        if (formatter != null) {
            return formatter.formatCellValue(cell, evaluator).trim();
        }
        return switch (cell.getCellType()) {
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val)) {
                    yield String.valueOf((long) val);
                }
                yield String.valueOf(val);
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> "";
        };
    }

    private LoteResponse toResponse(Lote lote) {
        long quantidadeGuias = guiaRepository.countByLoteId(lote.getId());
        return new LoteResponse(lote.getId(), lote.getNumeroLote(), lote.getCompetenciaAno(),
                lote.getCompetenciaMes(), lote.getDataGeracao(), lote.getFaturaNomeArquivo(),
                lote.getEspelhoNomeArquivo(), quantidadeGuias);
    }
}
