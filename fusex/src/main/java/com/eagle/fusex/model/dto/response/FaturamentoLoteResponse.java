package com.eagle.fusex.model.dto.response;

import com.eagle.fusex.model.enums.StatusLote;

public record FaturamentoLoteResponse(
        Long id,
        Integer numeroLote,
        long quantidadeGuias,
        String faturaNomeArquivo,
        String espelhoNomeArquivo,
        String numeroSisfat,
        StatusLote status) {
}
