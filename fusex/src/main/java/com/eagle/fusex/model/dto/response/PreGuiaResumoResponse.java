package com.eagle.fusex.model.dto.response;

import java.time.LocalDateTime;

public record PreGuiaResumoResponse(
        Long id,
        String token,
        String beneficiarioNome,
        String beneficiarioPrec,
        String ocsNome,
        LocalDateTime dataRegistro,
        LocalDateTime dataEmissao,
        boolean possuiEncaminhamento,
        boolean enviadaParaAssinatura,
        String numeroGuia,
        String situacaoAssinatura
) {
}
