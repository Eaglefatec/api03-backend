package com.eagle.fusex.service.importacao;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.model.dto.response.ImportacaoResumoResponse;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.OcsProcedimento;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.model.enums.Especialidade;
import com.eagle.fusex.model.enums.OrigemProcedimento;
import com.eagle.fusex.model.enums.TipoOcs;
import com.eagle.fusex.repository.OcsProcedimentoRepository;
import com.eagle.fusex.repository.OcsRepository;
import com.eagle.fusex.repository.ProcedimentoRepository;
import com.eagle.fusex.service.importacao.util.PlanilhaUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class OcsPrecoImportacaoService {

    private static final Map<String, Especialidade> MAPA_ESPECIALIDADE = Map.ofEntries(
            Map.entry("cardiologia", Especialidade.CARDIOLOGISTA),
            Map.entry("ortopedia", Especialidade.ORTOPEDISTA),
            Map.entry("endocrinologia", Especialidade.ENDOCRINOLOGISTA),
            Map.entry("ginecologia", Especialidade.GINECOLOGISTA_OBSTETRA),
            Map.entry("obstetricia", Especialidade.GINECOLOGISTA_OBSTETRA),
            Map.entry("ginecologia e obstetricia", Especialidade.GINECOLOGISTA_OBSTETRA),
            Map.entry("nutricao", Especialidade.NUTRICIONISTA),
            Map.entry("psiquiatria", Especialidade.PSIQUIATRA),
            Map.entry("dermatologia", Especialidade.DERMATOLOGISTA)
    );

    private final OcsRepository ocsRepository;
    private final ProcedimentoRepository procedimentoRepository;
    private final OcsProcedimentoRepository ocsProcedimentoRepository;

    public OcsPrecoImportacaoService(OcsRepository ocsRepository, ProcedimentoRepository procedimentoRepository,
                                     OcsProcedimentoRepository ocsProcedimentoRepository) {
        this.ocsRepository = ocsRepository;
        this.procedimentoRepository = procedimentoRepository;
        this.ocsProcedimentoRepository = ocsProcedimentoRepository;
    }

    @Transactional
    public ImportacaoResumoResponse importar(MultipartFile arquivo) {
        validarArquivo(arquivo);

        Map<String, String> indiceProcedimentos = new HashMap<>();
        AtomicInteger contadorLocal = new AtomicInteger(0);
        for (Procedimento procedimento : procedimentoRepository.findAll()) {
            indiceProcedimentos.putIfAbsent(PlanilhaUtils.normalizar(procedimento.getProcDescricao()), procedimento.getProcCodigoDgp());
            if (procedimento.getProcCodigoDgp().startsWith("LOCAL-")) {
                int numero = Integer.parseInt(procedimento.getProcCodigoDgp().substring(6));
                contadorLocal.updateAndGet(atual -> Math.max(atual, numero));
            }
        }

        int ocsProcessadas = 0;
        int precosImportados = 0;
        List<String> avisos = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(arquivo.getInputStream())) {
            for (int s = 0; s < workbook.getNumberOfSheets(); s++) {
                String nomeAba = workbook.getSheetName(s);
                if (nomeAba.startsWith("Planilha") || PlanilhaUtils.normalizar(nomeAba).equals("inicio")) {
                    continue;
                }

                Sheet sheet = workbook.getSheetAt(s);
                ResultadoAba resultado = processarAba(sheet, indiceProcedimentos, contadorLocal);
                if (resultado.motivo() != null) {
                    avisos.add("Aba '" + nomeAba + "': " + resultado.motivo());
                    continue;
                }

                ocsProcessadas++;
                precosImportados += resultado.precosImportados();
            }
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler a planilha de preços por OCS");
        }

        String mensagem = ocsProcessadas + " OCS processadas, " + precosImportados + " preços de procedimento importados";
        return new ImportacaoResumoResponse(mensagem, avisos);
    }

    private ResultadoAba processarAba(Sheet sheet, Map<String, String> indiceProcedimentos, AtomicInteger contadorLocal) {
        Integer linhaTabela1 = acharLinhaComTexto(sheet, "Tabela 1", 0);
        if (linhaTabela1 == null) {
            return new ResultadoAba(0, "não encontrou 'Tabela 1'");
        }

        int linhaDados1 = linhaTabela1 + 3;
        Row dados1 = sheet.getRow(linhaDados1);
        String ocsNome = PlanilhaUtils.textoDaCelula(dados1, 0);
        if (ocsNome == null || ocsNome.isBlank()) {
            return new ResultadoAba(0, "nome da OCS vazio");
        }

        String ocsTipoTexto = PlanilhaUtils.textoDaCelula(dados1, 1);
        String contrato = PlanilhaUtils.textoDaCelula(dados1, 2);
        LocalDate inicio = PlanilhaUtils.dataDaCelula(dados1, 3);
        LocalDate termino = PlanilhaUtils.dataDaCelula(dados1, 4);
        TipoOcs tipo = ocsTipoTexto != null && ocsTipoTexto.toUpperCase().contains("PSA") ? TipoOcs.PSA : TipoOcs.OCS;

        Ocs ocs = ocsRepository.findByOcsNome(ocsNome).orElseGet(Ocs::new);
        ocs.setOcsNome(ocsNome);
        ocs.setOcsTipo(tipo);
        ocs.setOcsContratoNumero(contrato);
        ocs.setOcsInicioVigencia(inicio);
        ocs.setOcsTerminoVigencia(termino);
        ocs.setEspecialidades(extrairEspecialidades(sheet, linhaDados1));
        ocs = ocsRepository.save(ocs);

        Integer linhaTabela2 = acharLinhaComTexto(sheet, "Tabela 2", linhaDados1);
        if (linhaTabela2 == null) {
            return new ResultadoAba(0, null);
        }
        Integer linhaHeader2 = acharLinhaComTexto(sheet, "Nr Ordem", linhaTabela2);
        if (linhaHeader2 == null) {
            return new ResultadoAba(0, null);
        }

        int precos = importarPrecos(sheet, linhaHeader2 + 1, ocs, indiceProcedimentos, contadorLocal);
        return new ResultadoAba(precos, null);
    }

    private Set<Especialidade> extrairEspecialidades(Sheet sheet, int aPartirDe) {
        Set<Especialidade> especialidades = new HashSet<>();
        Integer linhaLabel = acharLinhaComTexto(sheet, "Especialidades", aPartirDe);
        if (linhaLabel != null) {
            int ultimaLinha = sheet.getLastRowNum();
            for (int i = linhaLabel + 1; i <= ultimaLinha; i++) {
                Row row = sheet.getRow(i);
                String valor = PlanilhaUtils.textoDaCelula(row, 0);
                if (valor == null || valor.isBlank()) {
                    break;
                }
                if (PlanilhaUtils.normalizar(valor).contains("tabela 2")) {
                    break;
                }
                especialidades.add(MAPA_ESPECIALIDADE.getOrDefault(PlanilhaUtils.normalizar(valor), Especialidade.OUTROS));
            }
        }
        if (especialidades.isEmpty()) {
            especialidades.add(Especialidade.OUTROS);
        }
        return especialidades;
    }

    private int importarPrecos(Sheet sheet, int linhaInicial, Ocs ocs, Map<String, String> indiceProcedimentos,
                               AtomicInteger contadorLocal) {
        int total = 0;
        String descricaoAtual = "";
        int ultimaLinha = sheet.getLastRowNum();

        for (int i = linhaInicial; i <= ultimaLinha; i++) {
            Row row = sheet.getRow(i);
            if (PlanilhaUtils.vazia(row, 0) && PlanilhaUtils.vazia(row, 2) && PlanilhaUtils.vazia(row, 4)) {
                break;
            }

            String descricaoCelula = PlanilhaUtils.textoDaCelula(row, 1);
            if (descricaoCelula != null && !descricaoCelula.isBlank()) {
                descricaoAtual = descricaoCelula;
            }

            String procedimentoNome = PlanilhaUtils.textoDaCelula(row, 2);
            BigDecimal valor = PlanilhaUtils.valorDaCelula(row, 4);
            String tabelaReferencia = PlanilhaUtils.textoDaCelula(row, 5);

            if (procedimentoNome == null || procedimentoNome.isBlank() || valor == null) {
                continue;
            }

            String chave = PlanilhaUtils.normalizar(procedimentoNome);
            Procedimento procedimento = null;
            String codigo = indiceProcedimentos.get(chave);
            if (codigo != null) {
                procedimento = procedimentoRepository.findById(codigo).orElse(null);
            }
            if (procedimento == null) {
                codigo = "LOCAL-%06d".formatted(contadorLocal.incrementAndGet());
                procedimento = new Procedimento(codigo, procedimentoNome, 1);
                procedimento.setProcOrigem(OrigemProcedimento.OCS_LOCAL);
                procedimento = procedimentoRepository.save(procedimento);
                indiceProcedimentos.put(chave, codigo);
            }

            OcsProcedimento ocsProcedimento = ocsProcedimentoRepository
                    .findByOcs_OcsIdAndProcedimento_ProcCodigoDgp(ocs.getOcsId(), procedimento.getProcCodigoDgp())
                    .orElseGet(OcsProcedimento::new);
            ocsProcedimento.setOcs(ocs);
            ocsProcedimento.setProcedimento(procedimento);
            ocsProcedimento.setValor(valor);
            ocsProcedimento.setTabelaReferencia(tabelaReferencia);
            ocsProcedimento.setDescricaoGrupo(descricaoAtual.isBlank() ? null : descricaoAtual);
            ocsProcedimentoRepository.save(ocsProcedimento);

            total++;
        }

        return total;
    }

    private Integer acharLinhaComTexto(Sheet sheet, String texto, int aPartirDe) {
        String alvo = PlanilhaUtils.normalizar(texto);
        int ultimaLinha = sheet.getLastRowNum();
        for (int i = aPartirDe; i <= ultimaLinha; i++) {
            String valor = PlanilhaUtils.normalizar(PlanilhaUtils.textoDaCelula(sheet.getRow(i), 0));
            if (valor.contains(alvo)) {
                return i;
            }
        }
        return null;
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Selecione a planilha de preços por OCS");
        }
        String nome = arquivo.getOriginalFilename();
        String nomeMinusculo = nome != null ? nome.toLowerCase() : "";
        if (!nomeMinusculo.endsWith(".xls") && !nomeMinusculo.endsWith(".xlsx")) {
            throw new ArquivoInvalidoException("Envie a planilha de preços por OCS em formato .xls ou .xlsx");
        }
    }

    private record ResultadoAba(int precosImportados, String motivo) {
    }
}
