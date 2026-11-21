package com.eagle.fusex.service;

import com.eagle.fusex.exception.ResourceNotFoundException;
import com.eagle.fusex.model.dto.request.GlosaRequest;
import com.eagle.fusex.model.dto.response.DivergenciaResponse;
import com.eagle.fusex.model.dto.response.FaturamentoLoteResponse;
import com.eagle.fusex.model.entity.Fechamento;
import com.eagle.fusex.model.entity.Glosa;
import com.eagle.fusex.model.entity.Guia;
import com.eagle.fusex.model.entity.GuiaItem;
import com.eagle.fusex.model.entity.Lote;
import com.eagle.fusex.repository.FechamentoRepository;
import com.eagle.fusex.repository.GlosaRepository;
import com.eagle.fusex.repository.GuiaItemRepository;
import com.eagle.fusex.repository.GuiaRepository;
import com.eagle.fusex.repository.LoteRepository;
import com.eagle.fusex.security.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class LisuraService {

    private static final Logger log = LoggerFactory.getLogger(LisuraService.class);

    private final FechamentoRepository fechamentoRepository;
    private final GuiaRepository guiaRepository;
    private final GlosaRepository glosaRepository;
    private final LoteRepository loteRepository;
    private final GuiaItemRepository guiaItemRepository;

    public LisuraService(FechamentoRepository fechamentoRepository,
                         GuiaRepository guiaRepository,
                         GlosaRepository glosaRepository) {
        this(fechamentoRepository, guiaRepository, glosaRepository, null, null);
    }

    @Autowired
    public LisuraService(FechamentoRepository fechamentoRepository,
                         GuiaRepository guiaRepository,
                         GlosaRepository glosaRepository,
                         LoteRepository loteRepository,
                         GuiaItemRepository guiaItemRepository) {
        this.fechamentoRepository = fechamentoRepository;
        this.guiaRepository = guiaRepository;
        this.glosaRepository = glosaRepository;
        this.loteRepository = loteRepository;
        this.guiaItemRepository = guiaItemRepository;
    }

    @Transactional(readOnly = true)
    public List<Fechamento> listarFechamentosEmAuditoria() {
        return fechamentoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listarFechamentosFormatados() {
        List<Fechamento> lista = fechamentoRepository.findAll();
        List<Map<String, Object>> resposta = new ArrayList<>();

        for (Fechamento f : lista) {
            Map<String, Object> map = new HashMap<>();
            map.put("id", f.getId());
            map.put("competenciaMes", f.getCompetenciaMes());
            map.put("competenciaAno", f.getCompetenciaAno());
            map.put("dataEnvio", f.getDataEnvio());
            map.put("status", f.getStatus());

            List<Lote> lotes = loteRepository != null ? loteRepository.findByFechamentoId(f.getId()) : List.of();
            long quantidadeGuias = lotes.stream()
                    .mapToLong(lote -> guiaRepository.countByLoteId(lote.getId()))
                    .sum();
            map.put("quantidadeLotes", lotes.size());
            map.put("quantidadeGuias", quantidadeGuias);

            Map<String, Object> ocsMap = new HashMap<>();
            if (f.getOcs() != null) {
                ocsMap.put("ocsId", f.getOcs().getOcsId());
                ocsMap.put("ocsNome", f.getOcs().getOcsNome());
            }
            map.put("ocs", ocsMap);

            resposta.add(map);
        }

        return resposta;
    }

    @Transactional(readOnly = true)
    public List<FaturamentoLoteResponse> listarLotesPorFechamento(Long fechamentoId) {
        if (!fechamentoRepository.existsById(fechamentoId)) {
            throw new ResourceNotFoundException("Fechamento", fechamentoId);
        }
        if (loteRepository == null) {
            return List.of();
        }
        return loteRepository.findByFechamentoId(fechamentoId).stream()
                .map(lote -> new FaturamentoLoteResponse(
                        lote.getId(),
                        lote.getNumeroLote(),
                        guiaRepository.countByLoteId(lote.getId()),
                        lote.getFaturaNomeArquivo(),
                        lote.getEspelhoNomeArquivo(),
                        lote.getNumeroSisfat(),
                        lote.getStatus()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<GuiaItem> listarItensDaGuia(Long guiaId) {
        if (guiaItemRepository == null) {
            return List.of();
        }
        return guiaItemRepository.findByGuiaId(guiaId);
    }

    @Transactional(readOnly = true)
    public List<DivergenciaResponse> executarCruzamentoDados(Long fechamentoId) {
        Fechamento fechamento = fechamentoRepository.findById(fechamentoId)
                .orElseThrow(() -> new ResourceNotFoundException("Fechamento", fechamentoId));

        if (fechamento.getOcs() == null) {
            return new ArrayList<>();
        }

        Long ocsId = fechamento.getOcs().getOcsId();

        List<Guia> guias = guiaRepository.findByFechamentoId(fechamentoId);
        if (guias == null || guias.isEmpty()) {
            guias = guiaRepository.findByOcs_Id(ocsId);
        }

        if (guias == null || guias.isEmpty()) {
            return new ArrayList<>();
        }

        List<DivergenciaResponse> divergencias = new ArrayList<>(guias.size());

        for (Guia guia : guias) {
            BigDecimal valorContratado = guia.getValorContratado() != null ? guia.getValorContratado() : BigDecimal.ZERO;
            BigDecimal valorFaturado = guia.getValorFaturado() != null ? guia.getValorFaturado() : BigDecimal.ZERO;

            BigDecimal diferenca = valorFaturado.subtract(valorContratado);
            String status = diferenca.compareTo(BigDecimal.ZERO) != 0 ? "DIVERGENTE" : "APROVADO";

            String nomeBeneficiario = guia.getBeneficiarioNome() != null ? guia.getBeneficiarioNome() : "Não informado";
            String precCp = guia.getBeneficiarioPrecCp() != null ? guia.getBeneficiarioPrecCp() : "";
            String codigoTuss = guia.getCodigoTuss() != null ? guia.getCodigoTuss() : "N/A";

            divergencias.add(new DivergenciaResponse(
                    guia.getId(),
                    guia.getNumeroGuia(),
                    codigoTuss,
                    nomeBeneficiario,
                    precCp,
                    valorFaturado,
                    valorContratado,
                    diferenca,
                    status
            ));
        }

        return divergencias;
    }

    @Transactional
    public void registrarGlosa(GlosaRequest request) {
        String auditorResponsavel = SecurityUtils.getAuthenticatedUsername()
                .orElse("SISTEMA_AUDITOR");

        Guia guia = guiaRepository.findById(request.getGuiaId())
                .orElseThrow(() -> new ResourceNotFoundException("Guia", request.getGuiaId()));

        Glosa glosa = new Glosa();
        glosa.setGuia(guia);
        glosa.setCodigoTuss(request.getCodigoTuss());
        glosa.setJustificativaMilitar(request.getJustificativaMilitar());
        glosa.setValorGlosa(request.getValorGlosa());
        glosa.setConclusao(request.getConclusao());
        glosa.setAuditor(auditorResponsavel);

        glosaRepository.save(glosa);

        log.info("Glosa registrada para a Guia ID {} por {} | Valor: R$ {}",
                request.getGuiaId(), auditorResponsavel, request.getValorGlosa());
    }
}