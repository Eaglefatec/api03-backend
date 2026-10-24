package com.eagle.fusex.service.importacao;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.model.dto.response.ImportacaoResumoResponse;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.model.enums.OrigemProcedimento;
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
import java.util.ArrayList;
import java.util.List;

@Service
public class TussImportacaoService {

    private final ProcedimentoRepository procedimentoRepository;

    public TussImportacaoService(ProcedimentoRepository procedimentoRepository) {
        this.procedimentoRepository = procedimentoRepository;
    }

    @Transactional
    public ImportacaoResumoResponse importar(MultipartFile arquivo) {
        validarArquivo(arquivo);

        int importados = 0;
        int atualizados = 0;
        List<String> avisos = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int ultimaLinha = sheet.getLastRowNum();

            for (int i = 2; i <= ultimaLinha; i++) {
                Row row = sheet.getRow(i);
                String codigo = PlanilhaUtils.textoDaCelula(row, 0);
                String descricao = PlanilhaUtils.textoDaCelula(row, 1);

                if (codigo == null || descricao == null || descricao.isBlank() || !codigo.matches("\\d+")) {
                    continue;
                }

                boolean existia = procedimentoRepository.existsById(codigo);
                Procedimento procedimento = procedimentoRepository.findById(codigo)
                        .orElseGet(() -> new Procedimento(codigo, descricao, 1));
                procedimento.setProcDescricao(descricao);
                procedimento.setProcOrigem(OrigemProcedimento.TUSS);
                procedimentoRepository.save(procedimento);

                if (existia) {
                    atualizados++;
                } else {
                    importados++;
                }
            }
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler a planilha TUSS");
        }

        String mensagem = importados + " procedimentos importados, " + atualizados + " atualizados";
        return new ImportacaoResumoResponse(mensagem, avisos);
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Selecione a planilha do Rol TUSS");
        }
        String nome = arquivo.getOriginalFilename();
        String nomeMinusculo = nome != null ? nome.toLowerCase() : "";
        if (!nomeMinusculo.endsWith(".xls") && !nomeMinusculo.endsWith(".xlsx")) {
            throw new ArquivoInvalidoException("Envie a planilha do Rol TUSS em formato .xls ou .xlsx");
        }
    }
}
