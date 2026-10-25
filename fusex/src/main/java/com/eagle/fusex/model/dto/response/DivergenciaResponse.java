package com.eagle.fusex.model.dto.response;

import java.math.BigDecimal;

public class DivergenciaResponse {
    private Long guiaId;
    private String numeroGuia;
    private String codigoTuss;
    private String beneficiarioNome;
    private String beneficiarioPrecCp;
    private BigDecimal valorFaturado;
    private BigDecimal valorContratado;
    private BigDecimal diferenca;
    private String statusDivergencia;

    // Construtor atualizado
    public DivergenciaResponse(Long guiaId, String numeroGuia, String codigoTuss, String beneficiarioNome, String beneficiarioPrecCp, BigDecimal valorFaturado, BigDecimal valorContratado, BigDecimal diferenca, String statusDivergencia) {
        this.guiaId = guiaId;
        this.numeroGuia = numeroGuia;
        this.codigoTuss = codigoTuss;
        this.beneficiarioNome = beneficiarioNome;
        this.beneficiarioPrecCp = beneficiarioPrecCp;
        this.valorFaturado = valorFaturado;
        this.valorContratado = valorContratado;
        this.diferenca = diferenca;
        this.statusDivergencia = statusDivergencia;
    }

    // Getters
    public Long getGuiaId() { return guiaId; }
    public String getNumeroGuia() { return numeroGuia; }
    public String getCodigoTuss() { return codigoTuss; }
    public String getBeneficiarioNome() { return beneficiarioNome; }
    public String getBeneficiarioPrecCp() { return beneficiarioPrecCp; }
    public BigDecimal getValorFaturado() { return valorFaturado; }
    public BigDecimal getValorContratado() { return valorContratado; }
    public BigDecimal getDiferenca() { return diferenca; }
    public String getStatusDivergencia() { return statusDivergencia; }
}