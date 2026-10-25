package com.eagle.fusex.model.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public class GlosaRequest {

    @NotNull(message = "O ID da guia é obrigatório")
    private Long guiaId;

    private String codigoTuss;

    @NotNull(message = "A justificativa militar é obrigatória")
    private String justificativaMilitar;

    @NotNull(message = "O valor da glosa é obrigatório")
    private BigDecimal valorGlosa;

    // Getters e Setters
    public Long getGuiaId() { return guiaId; }
    public void setGuiaId(Long guiaId) { this.guiaId = guiaId; }

    public String getCodigoTuss() { return codigoTuss; }
    public void setCodigoTuss(String codigoTuss) { this.codigoTuss = codigoTuss; }

    public String getJustificativaMilitar() { return justificativaMilitar; }
    public void setJustificativaMilitar(String justificativaMilitar) { this.justificativaMilitar = justificativaMilitar; }

    public BigDecimal getValorGlosa() { return valorGlosa; }
    public void setValorGlosa(BigDecimal valorGlosa) { this.valorGlosa = valorGlosa; }
}