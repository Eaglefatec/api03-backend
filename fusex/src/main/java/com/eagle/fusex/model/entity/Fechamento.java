package com.eagle.fusex.model.entity;

import com.eagle.fusex.model.enums.StatusFechamento;
import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "fechamento")
public class Fechamento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ocs_id", nullable = false)
    private Ocs ocs;

    @Column(name = "competencia_ano", nullable = false)
    private Integer competenciaAno;

    @Column(name = "competencia_mes", nullable = false)
    private Integer competenciaMes;

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusFechamento status;

    public Fechamento() {
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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Fechamento that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
