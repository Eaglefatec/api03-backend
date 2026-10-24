package com.eagle.fusex.model.dto.response;

public record ProcedimentoResponse(
        String procCodigoDgp,
        String procDescricao
) {
    public String getProcCodigoDgp() { return procCodigoDgp; }
    public String getProcDescricao() { return procDescricao; }
}
