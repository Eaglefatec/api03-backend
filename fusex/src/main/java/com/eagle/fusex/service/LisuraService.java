package com.eagle.fusex.service;

import com.eagle.fusex.model.dto.request.GlosaRequest;
import com.eagle.fusex.model.dto.response.DivergenciaResponse;
import com.eagle.fusex.model.entity.Fechamento;
import com.eagle.fusex.model.entity.Glosa;
import com.eagle.fusex.model.entity.Guia;
import com.eagle.fusex.repository.FechamentoRepository;
import com.eagle.fusex.repository.GlosaRepository;
import com.eagle.fusex.repository.GuiaRepository;
import com.eagle.fusex.security.SecurityUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
public class LisuraService {

    private final FechamentoRepository fechamentoRepository;
    private final GuiaRepository guiaRepository;
    private final GlosaRepository glosaRepository;

    public LisuraService(FechamentoRepository fechamentoRepository,
                         GuiaRepository guiaRepository,
                         GlosaRepository glosaRepository) {
        this.fechamentoRepository = fechamentoRepository;
        this.guiaRepository = guiaRepository;
        this.glosaRepository = glosaRepository;
    }

    @Transactional(readOnly = true)
    public List<Fechamento> listarFechamentosEmAuditoria() {
        return fechamentoRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DivergenciaResponse> executarCruzamentoDados(Long fechamentoId) {
        // 1. Busca o fechamento para identificar a OCS correspondente
        Fechamento fechamento = fechamentoRepository.findById(fechamentoId)
                .orElseThrow(() -> new RuntimeException("Fechamento não encontrado ID: " + fechamentoId));

        if (fechamento.getOcs() == null) {
            return new ArrayList<>();
        }

        // 2. Obtém o ID correto da OCS utilizando getOcsId() conforme definido na entidade Ocs.java
        Long ocsId = fechamento.getOcs().getOcsId();

        // 3. Busca APENAS as guias pertencentes à OCS deste fechamento
        List<Guia> guias = guiaRepository.findByOcs_Id(ocsId);

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
                .orElseThrow(() -> new RuntimeException("Guia não encontrada ID: " + request.getGuiaId()));

        Glosa glosa = new Glosa();
        glosa.setGuia(guia);
        glosa.setCodigoTuss(request.getCodigoTuss());
        glosa.setJustificativaMilitar(request.getJustificativaMilitar());
        glosa.setValorGlosa(request.getValorGlosa());

        glosaRepository.save(glosa);

        System.out.printf(">>> Glosa registada para a Guia ID %d por %s | Valor: R$ %s%n",
                request.getGuiaId(), auditorResponsavel, request.getValorGlosa());
    }
}