package com.eagle.fusex.service;

import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.exception.ValidationException;
import com.eagle.fusex.model.dto.request.ConfirmarFaturamentoRequest;
import com.eagle.fusex.model.dto.request.ConfirmarFaturamentoRequest.LoteSisfat;
import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.dto.response.FaturamentoFechamentoResponse;
import com.eagle.fusex.model.entity.Fechamento;
import com.eagle.fusex.model.entity.Lote;
import com.eagle.fusex.model.entity.Ocs;
import com.eagle.fusex.model.enums.StatusFechamento;
import com.eagle.fusex.model.enums.StatusLote;
import com.eagle.fusex.repository.FechamentoRepository;
import com.eagle.fusex.repository.GuiaRepository;
import com.eagle.fusex.repository.LoteRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FaturamentoServiceTest {

    @Mock
    private FechamentoRepository fechamentoRepository;

    @Mock
    private LoteRepository loteRepository;

    @Mock
    private GuiaRepository guiaRepository;

    private FaturamentoService service;
    private Fechamento fechamento;
    private Lote lote1;
    private Lote lote2;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new FaturamentoService(fechamentoRepository, loteRepository, guiaRepository);

        Ocs ocs = new Ocs();
        ocs.setOcsId(1L);
        ocs.setOcsNome("Plani Diagnósticos");

        fechamento = new Fechamento();
        fechamento.setId(10L);
        fechamento.setOcs(ocs);
        fechamento.setCompetenciaAno(2026);
        fechamento.setCompetenciaMes(10);
        fechamento.setStatus(StatusFechamento.AGUARDANDO_FATURAMENTO);

        lote1 = criarLote(1L, 1);
        lote2 = criarLote(2L, 2);

        when(fechamentoRepository.findById(10L)).thenReturn(Optional.of(fechamento));
        when(loteRepository.findByFechamentoId(10L)).thenReturn(List.of(lote1, lote2));
    }

    private Lote criarLote(Long id, int numero) {
        Lote lote = new Lote();
        lote.setId(id);
        lote.setNumeroLote(numero);
        lote.setFaturaNomeArquivo("fatura" + numero + ".pdf");
        lote.setEspelhoNomeArquivo("espelho" + numero + ".csv");
        return lote;
    }

    private ConfirmarFaturamentoRequest pedido(LoteSisfat... itens) {
        return new ConfirmarFaturamentoRequest(List.of(itens));
    }

    @Test
    void testListarFechamentosPendentes_RetornaResumoComQuantidades() {
        when(fechamentoRepository.findByStatusOrderByDataEnvioAsc(StatusFechamento.AGUARDANDO_FATURAMENTO))
                .thenReturn(List.of(fechamento));
        when(guiaRepository.countByLoteId(1L)).thenReturn(3L);
        when(guiaRepository.countByLoteId(2L)).thenReturn(2L);

        List<FaturamentoFechamentoResponse> resultado = service.listarFechamentosPendentes();

        assertEquals(1, resultado.size());
        assertEquals("Plani Diagnósticos", resultado.get(0).ocsNome());
        assertEquals(2, resultado.get(0).quantidadeLotes());
        assertEquals(5L, resultado.get(0).quantidadeGuias());
    }

    @Test
    void testListarLotes_FechamentoInexistente_LancaExcecao() {
        when(fechamentoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.listarLotes(99L));
    }

    @Test
    void testConfirmar_ComTodosOsNumeros_LiberaParaLisura() {
        when(loteRepository.existsByNumeroSisfat(anyString())).thenReturn(false);

        FaturamentoFechamentoResponse resposta = service.confirmar(10L,
                pedido(new LoteSisfat(1L, " SF-1 "), new LoteSisfat(2L, "SF-2")));

        assertEquals(StatusFechamento.AGUARDANDO_LISURA, resposta.status());
        assertEquals(StatusFechamento.AGUARDANDO_LISURA, fechamento.getStatus());
        assertEquals("SF-1", lote1.getNumeroSisfat());
        assertEquals("SF-2", lote2.getNumeroSisfat());
        assertEquals(StatusLote.AGUARDANDO_LISURA, lote1.getStatus());
        assertEquals(StatusLote.AGUARDANDO_LISURA, lote2.getStatus());
    }

    @Test
    void testConfirmar_FaltandoNumeroDeUmLote_LancaExcecao() {
        assertThrows(ValidationException.class,
                () -> service.confirmar(10L, pedido(new LoteSisfat(1L, "SF-1"))));
        assertNull(lote1.getNumeroSisfat());
        assertEquals(StatusFechamento.AGUARDANDO_FATURAMENTO, fechamento.getStatus());
    }

    @Test
    void testConfirmar_NumeroRepetidoNaMesmaRequisicao_LancaExcecao() {
        assertThrows(ValidationException.class,
                () -> service.confirmar(10L, pedido(new LoteSisfat(1L, "SF-1"), new LoteSisfat(2L, "SF-1 "))));
    }

    @Test
    void testConfirmar_NumeroJaUsadoEmOutroLote_LancaExcecao() {
        when(loteRepository.existsByNumeroSisfat("SF-1")).thenReturn(true);

        assertThrows(ValidationException.class,
                () -> service.confirmar(10L, pedido(new LoteSisfat(1L, "SF-1"), new LoteSisfat(2L, "SF-2"))));
        assertEquals(StatusFechamento.AGUARDANDO_FATURAMENTO, fechamento.getStatus());
    }

    @Test
    void testConfirmar_LoteDeOutroFechamento_LancaExcecao() {
        assertThrows(ValidationException.class,
                () -> service.confirmar(10L, pedido(new LoteSisfat(1L, "SF-1"), new LoteSisfat(99L, "SF-9"))));
    }

    @Test
    void testConfirmar_FechamentoJaFaturado_LancaExcecao() {
        fechamento.setStatus(StatusFechamento.AGUARDANDO_LISURA);

        assertThrows(ValidationException.class,
                () -> service.confirmar(10L, pedido(new LoteSisfat(1L, "SF-1"), new LoteSisfat(2L, "SF-2"))));
    }

    @Test
    void testObterFatura_LeConteudoDoArquivo(@TempDir Path pasta) throws Exception {
        Path pdf = pasta.resolve("fatura.pdf");
        Files.write(pdf, new byte[]{1, 2, 3});
        lote1.setFaturaCaminhoArquivo(pdf.toString());
        lote1.setFaturaTipoArquivo("application/pdf");
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote1));

        ArquivoLoteDownload arquivo = service.obterFatura(1L);

        assertArrayEquals(new byte[]{1, 2, 3}, arquivo.conteudo());
        assertEquals("fatura1.pdf", arquivo.nome());
        assertEquals("application/pdf", arquivo.tipo());
    }

    @Test
    void testObterEspelho_ArquivoAusenteNoArmazenamento_LancaExcecao() {
        lote1.setEspelhoCaminhoArquivo("pasta-que-nao-existe/espelho.csv");
        when(loteRepository.findById(1L)).thenReturn(Optional.of(lote1));

        assertThrows(ResourceNotFoundException.class, () -> service.obterEspelho(1L));
    }
}
