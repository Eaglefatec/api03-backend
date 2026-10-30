package com.eagle.fusex.model.dto.response;

import com.eagle.fusex.model.enums.StatusFechamento;

import java.time.LocalDateTime;

public record FaturamentoFechamentoResponse(
        Long id,
        String ocsNome,
        Integer competenciaAno,
        Integer competenciaMes,
        LocalDateTime dataEnvio,
        int quantidadeLotes,
        long quantidadeGuias,
        StatusFechamento status) {
}
