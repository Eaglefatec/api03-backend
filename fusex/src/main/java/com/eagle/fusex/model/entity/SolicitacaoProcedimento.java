package com.eagle.fusex.model.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "solicitacao_procedimento")
public class SolicitacaoProcedimento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "solicitacao_id", nullable = false)
    private SolicitacaoMedica solicitacaoMedica;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proc_codigo_dgp", nullable = false)
    private Procedimento procedimento;

    @Column(nullable = false)
    private Integer quantidade;

    @Column(name = "valor_no_momento", precision = 10, scale = 2)
    private BigDecimal valorNoMomento;

    public SolicitacaoProcedimento() {
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

    public Procedimento getProcedimento() {
        return procedimento;
    }

    public void setProcedimento(Procedimento procedimento) {
        this.procedimento = procedimento;
    }

    public Integer getQuantidade() {
        return quantidade;
    }

    public void setQuantidade(Integer quantidade) {
        this.quantidade = quantidade;
    }

    public BigDecimal getValorNoMomento() {
        return valorNoMomento;
    }

    public void setValorNoMomento(BigDecimal valorNoMomento) {
        this.valorNoMomento = valorNoMomento;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SolicitacaoProcedimento that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
