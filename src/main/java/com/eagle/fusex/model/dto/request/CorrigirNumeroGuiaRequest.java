package com.eagle.fusex.model.dto.request;

import jakarta.validation.constraints.NotBlank;

public class CorrigirNumeroGuiaRequest {

    @NotBlank(message = "numeroGuia é obrigatório")
    private String numeroGuia;

    public CorrigirNumeroGuiaRequest() {
    }

    public CorrigirNumeroGuiaRequest(String numeroGuia) {
        this.numeroGuia = numeroGuia;
    }

    public String getNumeroGuia() {
        return numeroGuia;
    }

    public void setNumeroGuia(String numeroGuia) {
        this.numeroGuia = numeroGuia;
    }
}
