package com.eagle.fusex.model.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "lote")
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ocs_id", nullable = false)
    private Ocs ocs;

    @Column(name = "numero_lote", nullable = false)
    private Integer numeroLote;

    @Column(name = "competencia_ano", nullable = false)
    private Integer competenciaAno;

    @Column(name = "competencia_mes", nullable = false)
    private Integer competenciaMes;

    @Column(name = "data_geracao", nullable = false)
    private LocalDateTime dataGeracao;

    @Column(name = "fatura_nome_arquivo", nullable = false)
    private String faturaNomeArquivo;

    @Column(name = "fatura_caminho_arquivo", nullable = false)
    private String faturaCaminhoArquivo;

    @Column(name = "fatura_tipo_arquivo", nullable = false)
    private String faturaTipoArquivo;

    @Column(name = "espelho_nome_arquivo", nullable = false)
    private String espelhoNomeArquivo;

    @Column(name = "espelho_caminho_arquivo", nullable = false)
    private String espelhoCaminhoArquivo;

    @Column(name = "espelho_tipo_arquivo", nullable = false)
    private String espelhoTipoArquivo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fechamento_id")
    private Fechamento fechamento;

    public Lote() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ocs getOcs() {
        return ocs;
    }

    public void setOcs(Ocs ocs) {
        this.ocs = ocs;
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

    public String getFaturaCaminhoArquivo() {
        return faturaCaminhoArquivo;
    }

    public void setFaturaCaminhoArquivo(String faturaCaminhoArquivo) {
        this.faturaCaminhoArquivo = faturaCaminhoArquivo;
    }

    public String getFaturaTipoArquivo() {
        return faturaTipoArquivo;
    }

    public void setFaturaTipoArquivo(String faturaTipoArquivo) {
        this.faturaTipoArquivo = faturaTipoArquivo;
    }

    public String getEspelhoNomeArquivo() {
        return espelhoNomeArquivo;
    }

    public void setEspelhoNomeArquivo(String espelhoNomeArquivo) {
        this.espelhoNomeArquivo = espelhoNomeArquivo;
    }

    public String getEspelhoCaminhoArquivo() {
        return espelhoCaminhoArquivo;
    }

    public void setEspelhoCaminhoArquivo(String espelhoCaminhoArquivo) {
        this.espelhoCaminhoArquivo = espelhoCaminhoArquivo;
    }

    public String getEspelhoTipoArquivo() {
        return espelhoTipoArquivo;
    }

    public void setEspelhoTipoArquivo(String espelhoTipoArquivo) {
        this.espelhoTipoArquivo = espelhoTipoArquivo;
    }

    public Fechamento getFechamento() {
        return fechamento;
    }

    public void setFechamento(Fechamento fechamento) {
        this.fechamento = fechamento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Lote lote)) return false;
        return Objects.equals(id, lote.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
