package com.eagle.fusex.model.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class OcsProcedimentoId implements Serializable {

    @Column(name = "ocs_id")
    private Long ocsId;

    @Column(name = "proc_codigo_dgp")
    private String procCodigoDgp;

    public OcsProcedimentoId() {
    }

    public OcsProcedimentoId(Long ocsId, String procCodigoDgp) {
        this.ocsId = ocsId;
        this.procCodigoDgp = procCodigoDgp;
    }

    public Long getOcsId() {
        return ocsId;
    }

    public void setOcsId(Long ocsId) {
        this.ocsId = ocsId;
    }

    public String getProcCodigoDgp() {
        return procCodigoDgp;
    }

    public void setProcCodigoDgp(String procCodigoDgp) {
        this.procCodigoDgp = procCodigoDgp;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OcsProcedimentoId that)) return false;
        return Objects.equals(ocsId, that.ocsId) && Objects.equals(procCodigoDgp, that.procCodigoDgp);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ocsId, procCodigoDgp);
    }
}
