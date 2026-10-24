package com.eagle.fusex.service.importacao;

import com.eagle.fusex.exception.ArquivoInvalidoException;
import com.eagle.fusex.model.dto.response.ImportacaoResumoResponse;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.repository.OcsRepository;
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
import java.util.Optional;

@Service
public class OcsCadastroImportacaoService {

    private final OcsRepository ocsRepository;

    public OcsCadastroImportacaoService(OcsRepository ocsRepository) {
        this.ocsRepository = ocsRepository;
    }

    @Transactional
    public ImportacaoResumoResponse importar(MultipartFile arquivo) {
        validarArquivo(arquivo);

        int atualizados = 0;
        List<String> avisos = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(arquivo.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            int ultimaLinha = sheet.getLastRowNum();

            for (int i = 1; i <= ultimaLinha; i++) {
                Row row = sheet.getRow(i);
                String nome = PlanilhaUtils.textoDaCelula(row, 0);
                if (nome == null || nome.isBlank()) {
                    continue;
                }

                Optional<Ocs> ocsExistente = ocsRepository.findByOcsNome(nome);
                if (ocsExistente.isEmpty()) {
                    avisos.add("OCS não encontrada no cadastro: " + nome);
                    continue;
                }

                Ocs ocs = ocsExistente.get();
                ocs.setOcsInscricaoFederal(PlanilhaUtils.textoDaCelula(row, 1));
                ocs.setOcsEndereco(PlanilhaUtils.textoDaCelula(row, 2));
                ocs.setOcsEnderecoNumero(PlanilhaUtils.textoDaCelula(row, 3));
                ocs.setOcsEnderecoBairro(PlanilhaUtils.textoDaCelula(row, 4));
                ocs.setOcsEnderecoCidade(PlanilhaUtils.textoDaCelula(row, 5));
                ocs.setOcsEnderecoUf(PlanilhaUtils.textoDaCelula(row, 6));
                ocs.setOcsEnderecoCep(PlanilhaUtils.textoDaCelula(row, 7));
                ocs.setOcsContatoNome(PlanilhaUtils.textoDaCelula(row, 8));
                ocs.setOcsContatoTelefone(PlanilhaUtils.textoDaCelula(row, 9));
                ocsRepository.save(ocs);
                atualizados++;
            }
        } catch (IOException e) {
            throw new ArquivoInvalidoException("Não foi possível ler a planilha de cadastro das OCS");
        }

        String mensagem = atualizados + " OCS atualizadas com CNPJ, endereço e telefone";
        return new ImportacaoResumoResponse(mensagem, avisos);
    }

    private void validarArquivo(MultipartFile arquivo) {
        if (arquivo.isEmpty()) {
            throw new ArquivoInvalidoException("Selecione a planilha de cadastro das OCS");
        }
        String nome = arquivo.getOriginalFilename();
        String nomeMinusculo = nome != null ? nome.toLowerCase() : "";
        if (!nomeMinusculo.endsWith(".xls") && !nomeMinusculo.endsWith(".xlsx")) {
            throw new ArquivoInvalidoException("Envie a planilha de cadastro das OCS em formato .xls ou .xlsx");
        }
    }
}
