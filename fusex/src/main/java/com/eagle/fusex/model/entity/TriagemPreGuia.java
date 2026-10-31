package com.eagle.fusex.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "triagem_pre_guia")
public class TriagemPreGuia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id", nullable = false, unique = true)
    private SolicitacaoMedica solicitacaoMedica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ocs_id", nullable = false)
    private Ocs ocs;

    @Column(nullable = false)
    private Boolean aceitoTermos;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    public TriagemPreGuia() {
    }

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public SolicitacaoMedica getSolicitacaoMedica() {
        return solicitacaoMedica;
    }

    public void setSolicitacaoMedica(SolicitacaoMedica solicitacaoMedica) {
        this.solicitacaoMedica = solicitacaoMedica;
    }

    public Ocs getOcs() {
        return ocs;
    }

    public void setOcs(Ocs ocs) {
        this.ocs = ocs;
    }

    public Boolean getAceitoTermos() {
        return aceitoTermos;
    }

    public void setAceitoTermos(Boolean aceitoTermos) {
        this.aceitoTermos = aceitoTermos;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TriagemPreGuia that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
