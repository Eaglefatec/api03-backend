package com.eagle.fusex.model.dto.response;

import java.time.LocalDateTime;

public class LoteResponse {

    private Long id;
    private Integer numeroLote;
    private Integer competenciaAno;
    private Integer competenciaMes;
    private LocalDateTime dataGeracao;
    private String faturaNomeArquivo;
    private String espelhoNomeArquivo;
    private long quantidadeGuias;

    public LoteResponse() {
    }

    public LoteResponse(Long id, Integer numeroLote, Integer competenciaAno, Integer competenciaMes,
                         LocalDateTime dataGeracao, String faturaNomeArquivo, String espelhoNomeArquivo,
                         long quantidadeGuias) {
        this.id = id;
        this.numeroLote = numeroLote;
        this.competenciaAno = competenciaAno;
        this.competenciaMes = competenciaMes;
        this.dataGeracao = dataGeracao;
        this.faturaNomeArquivo = faturaNomeArquivo;
        this.espelhoNomeArquivo = espelhoNomeArquivo;
        this.quantidadeGuias = quantidadeGuias;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getNumeroLote() {
        return numeroLote;
    }

    public void setNumeroLote(Integer numeroLote) {
        this.numeroLote = numeroLote;
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

    public LocalDateTime getDataGeracao() {
        return dataGeracao;
    }

    public void setDataGeracao(LocalDateTime dataGeracao) {
        this.dataGeracao = dataGeracao;
    }

    public String getFaturaNomeArquivo() {
        return faturaNomeArquivo;
    }

    public void setFaturaNomeArquivo(String faturaNomeArquivo) {
        this.faturaNomeArquivo = faturaNomeArquivo;
    }

    public String getEspelhoNomeArquivo() {
        return espelhoNomeArquivo;
    }

    public void setEspelhoNomeArquivo(String espelhoNomeArquivo) {
        this.espelhoNomeArquivo = espelhoNomeArquivo;
    }

    public long getQuantidadeGuias() {
        return quantidadeGuias;
    }

    public void setQuantidadeGuias(long quantidadeGuias) {
        this.quantidadeGuias = quantidadeGuias;
    }
}
