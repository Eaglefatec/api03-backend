package com.eagle.fusex.model.dto.response;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class GuiaImportacaoResponse {

    private String numeroGuia;
    private LocalDateTime dataImportacaoGuia;
    private LocalDate dataValidade;
    private boolean numeroIdentificado;
    private String mensagem;

    public GuiaImportacaoResponse() {
    }

    public GuiaImportacaoResponse(String numeroGuia, LocalDateTime dataImportacaoGuia, LocalDate dataValidade,
                                  boolean numeroIdentificado, String mensagem) {
        this.numeroGuia = numeroGuia;
        this.dataImportacaoGuia = dataImportacaoGuia;
        this.dataValidade = dataValidade;
        this.numeroIdentificado = numeroIdentificado;
        this.mensagem = mensagem;
    }

    public String getNumeroGuia() {
        return numeroGuia;
    }

    public void setNumeroGuia(String numeroGuia) {
        this.numeroGuia = numeroGuia;
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

    public boolean isNumeroIdentificado() {
        return numeroIdentificado;
    }

    public void setNumeroIdentificado(boolean numeroIdentificado) {
        this.numeroIdentificado = numeroIdentificado;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
