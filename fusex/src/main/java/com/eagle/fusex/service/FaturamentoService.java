package com.eagle.fusex.service;

import com.eagle.fusex.exception.BusinessException;
import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.model.dto.response.ArquivoLoteDownload;
import com.eagle.fusex.model.dto.response.FaturamentoFechamentoResponse;
import com.eagle.fusex.model.dto.response.FaturamentoLoteResponse;
import com.eagle.fusex.model.entity.Fechamento;
import com.eagle.fusex.model.entity.Lote;
import com.eagle.fusex.model.enums.StatusFechamento;
import com.eagle.fusex.repository.FechamentoRepository;
import com.eagle.fusex.repository.GuiaRepository;
import com.eagle.fusex.repository.LoteRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

@Service
public class FaturamentoService {

    private final FechamentoRepository fechamentoRepository;
    private final LoteRepository loteRepository;
    private final GuiaRepository guiaRepository;

    public FaturamentoService(FechamentoRepository fechamentoRepository, LoteRepository loteRepository,
                              GuiaRepository guiaRepository) {
        this.fechamentoRepository = fechamentoRepository;
        this.loteRepository = loteRepository;
        this.guiaRepository = guiaRepository;
    }

    @Transactional(readOnly = true)
    public List<FaturamentoFechamentoResponse> listarFechamentosPendentes() {
        return fechamentoRepository.findByStatusOrderByDataEnvioAsc(StatusFechamento.AGUARDANDO_FATURAMENTO).stream()
                .map(this::toFechamentoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FaturamentoLoteResponse> listarLotes(Long fechamentoId) {
        buscarFechamento(fechamentoId);
        return loteRepository.findByFechamentoId(fechamentoId).stream()
                .map(this::toLoteResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ArquivoLoteDownload obterFatura(Long loteId) {
        Lote lote = buscarLote(loteId);
        return lerArquivo(lote.getFaturaCaminhoArquivo(), lote.getFaturaNomeArquivo(), lote.getFaturaTipoArquivo());
    }

    @Transactional(readOnly = true)
    public ArquivoLoteDownload obterEspelho(Long loteId) {
        Lote lote = buscarLote(loteId);
        return lerArquivo(lote.getEspelhoCaminhoArquivo(), lote.getEspelhoNomeArquivo(), lote.getEspelhoTipoArquivo());
    }

    private Lote buscarLote(Long loteId) {
        return loteRepository.findById(loteId)
                .orElseThrow(() -> new ResourceNotFoundException("Lote", loteId));
    }

    private ArquivoLoteDownload lerArquivo(String caminho, String nome, String tipo) {
        Path arquivo = Path.of(caminho);
        if (!Files.exists(arquivo)) {
            throw new ResourceNotFoundException("Arquivo do lote não encontrado no armazenamento");
        }
        try {
            return new ArquivoLoteDownload(Files.readAllBytes(arquivo), nome, tipo);
        } catch (IOException e) {
            throw new BusinessException("Não foi possível ler o arquivo do lote", e);
        }
    }

    private Fechamento buscarFechamento(Long fechamentoId) {
        return fechamentoRepository.findById(fechamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Fechamento", fechamentoId));
    }

    private FaturamentoFechamentoResponse toFechamentoResponse(Fechamento fechamento) {
        List<Lote> lotes = loteRepository.findByFechamentoId(fechamento.getId());
        long quantidadeGuias = lotes.stream()
                .mapToLong(lote -> guiaRepository.countByLoteId(lote.getId()))
                .sum();
        return new FaturamentoFechamentoResponse(fechamento.getId(), fechamento.getOcs().getOcsNome(),
                fechamento.getCompetenciaAno(), fechamento.getCompetenciaMes(), fechamento.getDataEnvio(),
                lotes.size(), quantidadeGuias, fechamento.getStatus());
    }

    private FaturamentoLoteResponse toLoteResponse(Lote lote) {
        return new FaturamentoLoteResponse(lote.getId(), lote.getNumeroLote(),
                guiaRepository.countByLoteId(lote.getId()), lote.getFaturaNomeArquivo(),
                lote.getEspelhoNomeArquivo(), lote.getNumeroSisfat(), lote.getStatus());
    }
}
