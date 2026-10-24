package com.eagle.fusex.service.importacao;

import com.eagle.fusex.model.dto.response.ImportacaoResumoResponse;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.repository.OcsRepository;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OcsCadastroImportacaoServiceTest {

    @Mock
    private OcsRepository ocsRepository;

    private OcsCadastroImportacaoService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new OcsCadastroImportacaoService(ocsRepository);
    }

    private byte[] gerarPlanilha() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();

            Row header = sheet.createRow(0);
            header.createCell(0).setCellValue("ocs_nome");
            header.createCell(1).setCellValue("ocs_inscricao_federal");
            header.createCell(2).setCellValue("ocs_endereco");
            header.createCell(3).setCellValue("ocs_endereco_numero");
            header.createCell(4).setCellValue("ocs_endereco_bairro");
            header.createCell(5).setCellValue("ocs_endereco_cidade");
            header.createCell(6).setCellValue("ocs_endereco_uf");
            header.createCell(7).setCellValue("ocs_endereco_cep");
            header.createCell(8).setCellValue("ocs_contato_nome");
            header.createCell(9).setCellValue("ocs_contato_telefone");

            Row linha1 = sheet.createRow(1);
            linha1.createCell(0).setCellValue("CLINICA TESTE");
            linha1.createCell(1).setCellValue("18674947000224");
            linha1.createCell(2).setCellValue("Rua Teste");
            linha1.createCell(3).setCellValue("385");
            linha1.createCell(4).setCellValue("Centro");
            linha1.createCell(5).setCellValue("Cidade Teste");
            linha1.createCell(6).setCellValue("SP");
            linha1.createCell(7).setCellValue("12280050");
            linha1.createCell(9).setCellValue("1236255433");

            Row linha2 = sheet.createRow(2);
            linha2.createCell(0).setCellValue("CLINICA SEM CADASTRO");
            linha2.createCell(1).setCellValue("99999999000199");
            linha2.createCell(9).setCellValue("9999999999");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void testImportar_AtualizaOcsEncontradaEAvisaSemEncontrar() throws Exception {
        byte[] conteudo = gerarPlanilha();
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "cadastro.xlsx", "application/vnd.ms-excel", conteudo);

        Ocs ocsExistente = new Ocs();
        when(ocsRepository.findByOcsNome("CLINICA TESTE")).thenReturn(Optional.of(ocsExistente));
        when(ocsRepository.findByOcsNome("CLINICA SEM CADASTRO")).thenReturn(Optional.empty());
        when(ocsRepository.save(any(Ocs.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportacaoResumoResponse resposta = service.importar(arquivo);

        verify(ocsRepository, times(1)).save(any(Ocs.class));
        assertEquals("18674947000224", ocsExistente.getOcsInscricaoFederal());
        assertEquals("Rua Teste", ocsExistente.getOcsEndereco());
        assertEquals("1236255433", ocsExistente.getOcsContatoTelefone());
        assertEquals("1 OCS atualizadas com CNPJ, endereço e telefone", resposta.getMensagem());
        assertTrue(resposta.getAvisos().stream().anyMatch(a -> a.contains("CLINICA SEM CADASTRO")));
    }
}
