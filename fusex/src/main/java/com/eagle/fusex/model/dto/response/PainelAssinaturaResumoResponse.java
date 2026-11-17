package com.eagle.fusex.model.dto.response;

import java.util.List;

public record PainelAssinaturaResumoResponse(
        long pendentesMedico,
        long pendentesSecao,
        long prontasEnvio,
        long enviadas,
        List<GuiaAssinaturaResponse> guias
) {
}

