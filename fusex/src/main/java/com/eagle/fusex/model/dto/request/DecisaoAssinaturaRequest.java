package com.eagle.fusex.model.dto.request;

import com.eagle.fusex.model.enums.DecisaoAssinatura;
import jakarta.validation.constraints.NotNull;

public record DecisaoAssinaturaRequest(
        @NotNull(message = "A decisão é obrigatória")
        DecisaoAssinatura decisao,
        String perfil,
        Boolean confirmado,
        String assinaturaBase64,
        String motivoRejeicao
) {
}

