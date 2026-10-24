package com.eagle.fusex.service.importacao;

import com.eagle.fusex.model.dto.response.ImportacaoResumoResponse;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.repository.ProcedimentoRepository;
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

class TussImportacaoServiceTest {

    @Mock
    private ProcedimentoRepository procedimentoRepository;

    private TussImportacaoService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new TussImportacaoService(procedimentoRepository);
    }

    private byte[] gerarPlanilha() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet();
            sheet.createRow(0).createCell(0).setCellValue("Título");
            Row header = sheet.createRow(1);
            header.createCell(0).setCellValue("Código Tab 22");
            header.createCell(1).setCellValue("Descrição");

            Row linha1 = sheet.createRow(2);
            linha1.createCell(0).setCellValue("10101012");
            linha1.createCell(1).setCellValue("Consulta em consultorio");

            Row linha2 = sheet.createRow(3);
            linha2.createCell(0).setCellValue("40901106");
            linha2.createCell(1).setCellValue("Ecodopplercardiograma transtorácico");

            Row linhaInvalida = sheet.createRow(4);
            linhaInvalida.createCell(0).setCellValue("Nota de rodapé");
            linhaInvalida.createCell(1).setCellValue("Isso não é um procedimento");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void testImportar_ProcedimentosNovosSaoImportados() throws Exception {
        byte[] conteudo = gerarPlanilha();
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "tuss.xlsx", "application/vnd.ms-excel", conteudo);

        when(procedimentoRepository.existsById(any())).thenReturn(false);
        when(procedimentoRepository.findById(any())).thenReturn(Optional.empty());
        when(procedimentoRepository.save(any(Procedimento.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportacaoResumoResponse resposta = service.importar(arquivo);

        verify(procedimentoRepository, times(2)).save(any(Procedimento.class));
        assertTrue(resposta.getMensagem().contains("2 procedimentos importados"));
    }

    @Test
    void testImportar_ProcedimentoExistenteEhAtualizado() throws Exception {
        byte[] conteudo = gerarPlanilha();
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "tuss.xlsx", "application/vnd.ms-excel", conteudo);

        when(procedimentoRepository.existsById("10101012")).thenReturn(true);
        when(procedimentoRepository.existsById("40901106")).thenReturn(false);
        when(procedimentoRepository.findById("10101012"))
                .thenReturn(Optional.of(new Procedimento("10101012", "Consulta antiga", 1)));
        when(procedimentoRepository.findById("40901106")).thenReturn(Optional.empty());
        when(procedimentoRepository.save(any(Procedimento.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportacaoResumoResponse resposta = service.importar(arquivo);

        assertEquals("1 procedimentos importados, 1 atualizados", resposta.getMensagem());
    }
}
