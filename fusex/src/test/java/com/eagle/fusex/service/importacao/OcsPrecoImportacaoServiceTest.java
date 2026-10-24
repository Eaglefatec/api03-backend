package com.eagle.fusex.service.importacao;

import com.eagle.fusex.model.dto.response.ImportacaoResumoResponse;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.entity.OcsProcedimento;
import com.eagle.fusex.model.entity.Procedimento;
import com.eagle.fusex.repository.OcsProcedimentoRepository;
import com.eagle.fusex.repository.OcsRepository;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

class OcsPrecoImportacaoServiceTest {

    @Mock
    private OcsRepository ocsRepository;

    @Mock
    private ProcedimentoRepository procedimentoRepository;

    @Mock
    private OcsProcedimentoRepository ocsProcedimentoRepository;

    private OcsPrecoImportacaoService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new OcsPrecoImportacaoService(ocsRepository, procedimentoRepository, ocsProcedimentoRepository);
    }

    private byte[] gerarPlanilha() throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("CLINICA TESTE");

            sheet.createRow(0).createCell(0).setCellValue("Tabela 1");
            Row header1 = sheet.createRow(2);
            header1.createCell(0).setCellValue("Nome");
            header1.createCell(1).setCellValue("Tipo");
            header1.createCell(2).setCellValue("Contrato nº");
            header1.createCell(3).setCellValue("Início vigência");
            header1.createCell(4).setCellValue("Término vigência");

            Row dados1 = sheet.createRow(3);
            dados1.createCell(0).setCellValue("CLINICA TESTE");
            dados1.createCell(1).setCellValue("OCS");
            dados1.createCell(2).setCellValue("01/2023");

            sheet.createRow(4).createCell(0).setCellValue("Especialidades atendidas");
            sheet.createRow(5).createCell(0).setCellValue("Cardiologia");

            sheet.createRow(6).createCell(0).setCellValue("Tabela 2");
            Row header2 = sheet.createRow(7);
            header2.createCell(0).setCellValue("Nr Ordem");
            header2.createCell(1).setCellValue("Descrição");
            header2.createCell(2).setCellValue("Procedimento");
            header2.createCell(4).setCellValue("Valor (R$)");
            header2.createCell(5).setCellValue("Tabela/Outros");

            Row linha1 = sheet.createRow(8);
            linha1.createCell(0).setCellValue(1);
            linha1.createCell(1).setCellValue("Consultas");
            linha1.createCell(2).setCellValue("Consulta em consultorio");
            linha1.createCell(4).setCellValue(60.0);

            Row linha2 = sheet.createRow(9);
            linha2.createCell(0).setCellValue(2);
            linha2.createCell(2).setCellValue("Exame Novo Desconhecido");
            linha2.createCell(4).setCellValue(99.9);
            linha2.createCell(5).setCellValue("CBHPM 2012");

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    @Test
    void testImportar_ProcessaAbaComContratoEspecialidadesEPrecos() throws Exception {
        byte[] conteudo = gerarPlanilha();
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "precos.xlsx", "application/vnd.ms-excel", conteudo);

        Procedimento procedimentoTuss = new Procedimento("10101012", "Consulta em consultorio", 1);
        when(procedimentoRepository.findAll()).thenReturn(List.of(procedimentoTuss));
        when(procedimentoRepository.findById("10101012")).thenReturn(Optional.of(procedimentoTuss));
        when(procedimentoRepository.save(any(Procedimento.class))).thenAnswer(inv -> inv.getArgument(0));

        when(ocsRepository.findByOcsNome("CLINICA TESTE")).thenReturn(Optional.empty());
        when(ocsRepository.save(any(Ocs.class))).thenAnswer(inv -> {
            Ocs ocs = inv.getArgument(0);
            ocs.setOcsId(1L);
            return ocs;
        });

        when(ocsProcedimentoRepository.findByOcs_OcsIdAndProcedimento_ProcCodigoDgp(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        when(ocsProcedimentoRepository.save(any(OcsProcedimento.class))).thenAnswer(inv -> inv.getArgument(0));

        ImportacaoResumoResponse resposta = service.importar(arquivo);

        verify(ocsRepository, times(1)).save(any(Ocs.class));
        verify(ocsProcedimentoRepository, times(2)).save(any(OcsProcedimento.class));
        verify(procedimentoRepository, times(1)).save(argThat(p -> p.getProcCodigoDgp().startsWith("LOCAL-")));
        assertTrue(resposta.getMensagem().contains("1 OCS processadas"));
        assertTrue(resposta.getMensagem().contains("2 preços de procedimento importados"));
    }

    @Test
    void testImportar_AbaSemTabela1EhIgnoradaComAviso() throws Exception {
        byte[] conteudo;
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            workbook.createSheet("Clinica Sem Contrato").createRow(0).createCell(0).setCellValue("Observações");
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            conteudo = out.toByteArray();
        }
        MockMultipartFile arquivo = new MockMultipartFile("arquivo", "precos.xlsx", "application/vnd.ms-excel", conteudo);

        when(procedimentoRepository.findAll()).thenReturn(List.of());

        ImportacaoResumoResponse resposta = service.importar(arquivo);

        verify(ocsRepository, never()).save(any());
        assertTrue(resposta.getMensagem().contains("0 OCS processadas"));
        assertTrue(resposta.getAvisos().stream().anyMatch(a -> a.contains("Tabela 1")));
    }
}
