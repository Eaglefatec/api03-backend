package com.eagle.fusex.model.dto.response;

import com.eagle.fusex.model.enums.StatusGuia;

import java.time.LocalDate;

public class GuiaResponse {

    private Long id;
    private String numeroGuia;
    private String beneficiarioPrecCp;
    private LocalDate dataAtendimento;
    private String nomeArquivo;
    private StatusGuia status;

    public GuiaResponse() {
    }

    public GuiaResponse(Long id, String numeroGuia, String beneficiarioPrecCp, LocalDate dataAtendimento,
                         String nomeArquivo, StatusGuia status) {
        this.id = id;
        this.numeroGuia = numeroGuia;
        this.beneficiarioPrecCp = beneficiarioPrecCp;
        this.dataAtendimento = dataAtendimento;
        this.nomeArquivo = nomeArquivo;
        this.status = status;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroGuia() {
        return numeroGuia;
    }

    public void setNumeroGuia(String numeroGuia) {
        this.numeroGuia = numeroGuia;
    }

    public String getBeneficiarioPrecCp() {
        return beneficiarioPrecCp;
    }

    public void setBeneficiarioPrecCp(String beneficiarioPrecCp) {
        this.beneficiarioPrecCp = beneficiarioPrecCp;
    }

    public LocalDate getDataAtendimento() {
        return dataAtendimento;
    }

    public void setDataAtendimento(LocalDate dataAtendimento) {
        this.dataAtendimento = dataAtendimento;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public StatusGuia getStatus() {
        return status;
    }

    public void setStatus(StatusGuia status) {
        this.status = status;
    }
}
