package com.eagle.fusex.model.entity;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.Objects;

@Entity
@Table(name = "ocs_procedimento")
public class OcsProcedimento {

    @EmbeddedId
    private OcsProcedimentoId id = new OcsProcedimentoId();

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("ocsId")
    @JoinColumn(name = "ocs_id")
    private Ocs ocs;

    @ManyToOne(fetch = FetchType.LAZY)
    @MapsId("procCodigoDgp")
    @JoinColumn(name = "proc_codigo_dgp")
    private Procedimento procedimento;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valor;

    @Column(name = "tabela_referencia", length = 500)
    private String tabelaReferencia;

    @Column(name = "descricao_grupo", length = 500)
    private String descricaoGrupo;

    public OcsProcedimento() {
    }

    public OcsProcedimentoId getId() {
        return id;
    }

    public void setId(OcsProcedimentoId id) {
        this.id = id;
    }

    public Ocs getOcs() {
        return ocs;
    }

    public void setOcs(Ocs ocs) {
        this.ocs = ocs;
    }

    public Procedimento getProcedimento() {
        return procedimento;
    }

    public void setProcedimento(Procedimento procedimento) {
        this.procedimento = procedimento;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getTabelaReferencia() {
        return tabelaReferencia;
    }

    public void setTabelaReferencia(String tabelaReferencia) {
        this.tabelaReferencia = tabelaReferencia;
    }

    public String getDescricaoGrupo() {
        return descricaoGrupo;
    }

    public void setDescricaoGrupo(String descricaoGrupo) {
        this.descricaoGrupo = descricaoGrupo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OcsProcedimento that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
