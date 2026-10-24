package com.eagle.fusex.model.entity;

import com.eagle.fusex.model.enums.OrigemProcedimento;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "procedimento")
public class Procedimento {

    @Id
    @Column(name = "proc_codigo_dgp")
    private String procCodigoDgp;

    @Column(name = "proc_descricao", nullable = false, length = 1000)
    private String procDescricao;

    @Column(name = "proc_quantidade", nullable = false)
    private Integer procQuantidade = 1;

    @Enumerated(EnumType.STRING)
    @Column(name = "proc_origem", nullable = false)
    private OrigemProcedimento procOrigem = OrigemProcedimento.TUSS;

    @OneToMany(mappedBy = "procedimento")
    private Set<OcsProcedimento> ocsProcedimentos = new HashSet<>();

    public Procedimento() {
    }

    public Procedimento(String procCodigoDgp, String procDescricao, Integer procQuantidade) {
        this.procCodigoDgp = procCodigoDgp;
        this.procDescricao = procDescricao;
        this.procQuantidade = procQuantidade;
    }

    public String getProcCodigoDgp() {
        return procCodigoDgp;
    }

    public void setProcCodigoDgp(String procCodigoDgp) {
        this.procCodigoDgp = procCodigoDgp;
    }

    public String getProcDescricao() {
        return procDescricao;
    }

    public void setProcDescricao(String procDescricao) {
        this.procDescricao = procDescricao;
    }

    public Integer getProcQuantidade() {
        return procQuantidade;
    }

    public void setProcQuantidade(Integer procQuantidade) {
        this.procQuantidade = procQuantidade;
    }

    public OrigemProcedimento getProcOrigem() {
        return procOrigem;
    }

    public void setProcOrigem(OrigemProcedimento procOrigem) {
        this.procOrigem = procOrigem;
    }

    public Set<OcsProcedimento> getOcsProcedimentos() {
        return ocsProcedimentos;
    }

    public void setOcsProcedimentos(Set<OcsProcedimento> ocsProcedimentos) {
        this.ocsProcedimentos = ocsProcedimentos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Procedimento that)) return false;
        return Objects.equals(procCodigoDgp, that.procCodigoDgp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(procCodigoDgp);
    }
}
