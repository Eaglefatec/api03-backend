package com.eagle.fusex.model.enums;

public enum StatusEsteiraAssinatura {
    AGUARDANDO_MEDICO_CHEFE("AM", "Aguardando Médico Chefe"),
    AGUARDANDO_CHEFE_SECAO("AS", "Aguardando Chefe da Seção"),
    AGUARDANDO_ENVIO_BENEFICIARIO("PE", "Aguardando envio ao beneficiário"),
    ENVIADA_BENEFICIARIO("EN", "Enviada ao beneficiário"),
    REJEITADA("RJ", "Rejeitada");

    private final String codigoCurto;
    private final String descricao;

    StatusEsteiraAssinatura(String codigoCurto, String descricao) {
        this.codigoCurto = codigoCurto;
        this.descricao = descricao;
    }

    public String getCodigoCurto() {
        return codigoCurto;
    }

    public String getDescricao() {
        return descricao;
    }
}

