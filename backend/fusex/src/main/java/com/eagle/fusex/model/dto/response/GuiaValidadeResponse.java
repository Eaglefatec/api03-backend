package com.eagle.fusex.model.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class GuiaValidadeResponse {

    private String numeroGuia;
    private boolean valida;
    private String motivoInvalidez;
    private LocalDateTime dataImportacaoGuia;
    private LocalDate dataValidade;
    private String nomePaciente;
    private String ocsVinculada;

    public GuiaValidadeResponse() {
    }

    public GuiaValidadeResponse(String numeroGuia, boolean valida, String motivoInvalidez,
                                LocalDateTime dataImportacaoGuia, LocalDate dataValidade,
                                String nomePaciente, String ocsVinculada) {
        this.numeroGuia = numeroGuia;
        this.valida = valida;
        this.motivoInvalidez = motivoInvalidez;
        this.dataImportacaoGuia = dataImportacaoGuia;
        this.dataValidade = dataValidade;
        this.nomePaciente = nomePaciente;
        this.ocsVinculada = ocsVinculada;
    }

    public String getNumeroGuia() {
        return numeroGuia;
    }

    public void setNumeroGuia(String numeroGuia) {
        this.numeroGuia = numeroGuia;
    }

    public boolean isValida() {
        return valida;
    }

    public void setValida(boolean valida) {
        this.valida = valida;
    }

    public String getMotivoInvalidez() {
        return motivoInvalidez;
    }

    public void setMotivoInvalidez(String motivoInvalidez) {
        this.motivoInvalidez = motivoInvalidez;
    }

    public LocalDateTime getDataImportacaoGuia() {
        return dataImportacaoGuia;
    }

    public void setDataImportacaoGuia(LocalDateTime dataImportacaoGuia) {
        this.dataImportacaoGuia = dataImportacaoGuia;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public String getNomePaciente() {
        return nomePaciente;
    }

    public void setNomePaciente(String nomePaciente) {
        this.nomePaciente = nomePaciente;
    }

    public String getOcsVinculada() {
        return ocsVinculada;
    }

    public void setOcsVinculada(String ocsVinculada) {
        this.ocsVinculada = ocsVinculada;
    }
}
