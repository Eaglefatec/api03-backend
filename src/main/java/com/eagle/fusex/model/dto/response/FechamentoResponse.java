package com.eagle.fusex.model.dto.response;

import com.eagle.fusex.model.enums.StatusFechamento;

import java.time.LocalDateTime;

public class FechamentoResponse {

    private Long id;
    private Integer competenciaAno;
    private Integer competenciaMes;
    private LocalDateTime dataEnvio;
    private StatusFechamento status;
    private int quantidadeLotes;
    private long quantidadeGuias;

    public FechamentoResponse() {
    }

    public FechamentoResponse(Long id, Integer competenciaAno, Integer competenciaMes, LocalDateTime dataEnvio,
                               StatusFechamento status, int quantidadeLotes, long quantidadeGuias) {
        this.id = id;
        this.competenciaAno = competenciaAno;
        this.competenciaMes = competenciaMes;
        this.dataEnvio = dataEnvio;
        this.status = status;
        this.quantidadeLotes = quantidadeLotes;
        this.quantidadeGuias = quantidadeGuias;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getCompetenciaAno() {
        return competenciaAno;
    }

    public void setCompetenciaAno(Integer competenciaAno) {
        this.competenciaAno = competenciaAno;
    }

    public Integer getCompetenciaMes() {
        return competenciaMes;
    }

    public void setCompetenciaMes(Integer competenciaMes) {
        this.competenciaMes = competenciaMes;
    }

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDateTime dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public StatusFechamento getStatus() {
        return status;
    }

    public void setStatus(StatusFechamento status) {
        this.status = status;
    }

    public int getQuantidadeLotes() {
        return quantidadeLotes;
    }

    public void setQuantidadeLotes(int quantidadeLotes) {
        this.quantidadeLotes = quantidadeLotes;
    }

    public long getQuantidadeGuias() {
        return quantidadeGuias;
    }

    public void setQuantidadeGuias(long quantidadeGuias) {
        this.quantidadeGuias = quantidadeGuias;
    }
}
