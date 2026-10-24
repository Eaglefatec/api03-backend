package com.eagle.fusex.model.dto.response;

import com.eagle.fusex.model.entity.Medico;

public record MedicoResumoResponse(
        String nome,
        String crm
) {
    public static MedicoResumoResponse from(Medico medico) {
        if (medico == null) {
            return null;
        }
        return new MedicoResumoResponse(medico.getNome(), medico.getCrm());
    }

    public String getNome() { return nome; }
    public String getCrm() { return crm; }
}
