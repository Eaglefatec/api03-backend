package com.eagle.fusex.model.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class FechamentoRequest {

    @NotNull
    private Integer competenciaAno;

    @NotNull
    @Min(1)
    @Max(12)
    private Integer competenciaMes;

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
}
