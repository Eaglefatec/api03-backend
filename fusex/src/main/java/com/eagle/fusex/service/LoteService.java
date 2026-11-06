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
import com.eagle.fusex.model.enums.StatusGuia;
import com.eagle.fusex.repository.GuiaItemRepository;
import com.eagle.fusex.repository.GuiaRepository;
import com.eagle.fusex.repository.LoteRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.security.SecurityUtils;
import com.eagle.fusex.storage.FileStorageService;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVRecord;
import org.apache.poi.ss.usermodel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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

    public LoteService(LoteRepository loteRepository, GuiaRepository guiaRepository,
                       GuiaItemRepository guiaItemRepository, OcsRepository ocsRepository,
                       FileStorageService fileStorageService) {
        this.loteRepository = loteRepository;
        this.guiaRepository = guiaRepository;
        this.guiaItemRepository = guiaItemRepository;
        this.ocsRepository = ocsRepository;
        this.fileStorageService = fileStorageService;
    }

    @Transactional
    public LoteResponse gerarLote(List<Long> guiaIds, MultipartFile fatura, MultipartFile espelho) {
        Ocs ocs = buscarOcsLogada();

        if (guiaIds == null || guiaIds.isEmpty()) {
            throw new ValidationException("Selecione ao menos uma guia para gerar o lote");
        }

        List<Guia> guias = guiaRepository.findAllById(guiaIds);
        if (guias.size() != guiaIds.size()) {
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

        String faturaCaminho = fileStorageService.salvar(fatura, UUID.randomUUID() + "-" + extrairExtensao(fatura));
        String espelhoCaminho = fileStorageService.salvar(espelho, UUID.randomUUID() + "-" + extrairExtensao(espelho));

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

        // Mapeia as guias por número para associar os itens e atualizar os dados da guia por linha da planilha
        Map<String, Guia> guiasPorNumero = guias.stream()
                .collect(Collectors.toMap(Guia::getNumeroGuia, g -> g, (g1, g2) -> g1));

        // Processa o espelho (Excel/CSV), grava os itens e atualiza as guias
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

    // --- LEITURA, PERSISTÊNCIA DOS ITENS E ATUALIZAÇÃO DA GUIA ---

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

        try (Workbook workbook = WorkbookFactory.create(is)) {
            Sheet sheet = workbook.getSheetAt(0);
            log.info("Total de linhas no Excel do espelho: {}", sheet.getLastRowNum());

            for (Row row : sheet) {
                if (row.getRowNum() == 0) continue; // Pula o cabeçalho

                // Mapeamento exato com base nas colunas da imagem do espelho:
                String numeroGuia = getCellVal(row.getCell(0)); // Coluna 0: Nº da Guia
                String beneficiarioNome = getCellVal(row.getCell(1)); // Coluna 1: Beneficiário
                String precCp = getCellVal(row.getCell(2)); // Coluna 2: Prec CP
                String codigoTuss = getCellVal(row.getCell(3)); // Coluna 3: Código TUSS
                String descricao = getCellVal(row.getCell(4)); // Coluna 4: Descrição
                String tipo = getCellVal(row.getCell(5));       // Coluna 5: Tipo

                int quantidade = 1;
                try {
                    Cell cellQtd = row.getCell(6); // Coluna 6: Qtd
                    if (cellQtd != null) {
                        quantidade = (int) cellQtd.getNumericCellValue();
                    }
                } catch (Exception ignored) {}

                BigDecimal valorUnitario = BigDecimal.ZERO;
                try {
                    Cell cellVal = row.getCell(7); // Coluna 7: Valor Unit. (R$)
                    if (cellVal != null) {
                        valorUnitario = BigDecimal.valueOf(cellVal.getNumericCellValue());
                    }
                } catch (Exception ignored) {}

                if (!numeroGuia.isEmpty() && guiasPorNumero.containsKey(numeroGuia)) {
                    Guia guia = guiasPorNumero.get(numeroGuia);

                    // Atualiza os dados da entidade Guia com as informações da planilha
                    if (!beneficiarioNome.isEmpty()) guia.setBeneficiarioNome(beneficiarioNome);
                    if (!precCp.isEmpty()) guia.setBeneficiarioPrecCp(precCp);
                    if (!codigoTuss.isEmpty()) guia.setCodigoTuss(codigoTuss);

                    // Calcula o valor faturado da guia com base no item ou acumula se houver múltiplos itens
                    BigDecimal valorTotalItem = valorUnitario.multiply(BigDecimal.valueOf(quantidade));
                    guia.setValorFaturado(guia.getValorFaturado().add(valorTotalItem));

                    if (!guiasParaAtualizar.contains(guia)) {
                        guiasParaAtualizar.add(guia);
                    }

                    // Cria e adiciona o item da guia
                    GuiaItem item = new GuiaItem();
                    item.setGuiaId(guia.getId());
                    item.setCodigoTuss(codigoTuss);
                    item.setDescricaoItem(descricao.isEmpty() ? "Item sem descrição" : descricao);
                    item.setTipo(tipo.isEmpty() ? "Procedimento" : tipo);
                    item.setQuantidade(quantidade > 0 ? quantidade : 1);
                    item.setValorUnitario(valorUnitario);
                    item.setDataItem(LocalDate.now());

                    itensParaSalvar.add(item);
                } else {
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

        try (Reader reader = new InputStreamReader(is, StandardCharsets.UTF_8)) {
            Iterable<CSVRecord> records = CSVFormat.DEFAULT.withFirstRecordAsHeader().parse(reader);
            for (CSVRecord record : records) {
                String numeroGuia = record.get(0);
                String beneficiarioNome = record.get(1);
                String precCp = record.get(2);
                String codigoTuss = record.get(3);
                String descricao = record.get(4);
                String tipo = record.get(5);

                int quantidade = 1;
                try { quantidade = Integer.parseInt(record.get(6)); } catch (Exception ignored) {}

                BigDecimal valorUnitario = BigDecimal.ZERO;
                try { valorUnitario = new BigDecimal(record.get(7)); } catch (Exception ignored) {}

                if (numeroGuia != null && guiasPorNumero.containsKey(numeroGuia)) {
                    Guia guia = guiasPorNumero.get(numeroGuia);

                    if (beneficiarioNome != null) guia.setBeneficiarioNome(beneficiarioNome);
                    if (precCp != null) guia.setBeneficiarioPrecCp(precCp);
                    if (codigoTuss != null) guia.setCodigoTuss(codigoTuss);

                    BigDecimal valorTotalItem = valorUnitario.multiply(BigDecimal.valueOf(quantidade));
                    guia.setValorFaturado(guia.getValorFaturado().add(valorTotalItem));

                    if (!guiasParaAtualizar.contains(guia)) {
                        guiasParaAtualizar.add(guia);
                    }

                    GuiaItem item = new GuiaItem();
                    item.setGuiaId(guia.getId());
                    item.setCodigoTuss(codigoTuss);
                    item.setDescricaoItem(descricao);
                    item.setTipo(tipo);
                    item.setQuantidade(quantidade);
                    item.setValorUnitario(valorUnitario);
                    item.setDataItem(LocalDate.now());

                    itensParaSalvar.add(item);
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

    private String getCellVal(Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
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