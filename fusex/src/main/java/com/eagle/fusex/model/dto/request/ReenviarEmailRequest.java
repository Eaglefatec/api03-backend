package com.eagle.fusex.model.dto.request;

public class ReenviarEmailRequest {

    private String novoEmail;

    public ReenviarEmailRequest() {
    }

    public ReenviarEmailRequest(String novoEmail) {
        this.novoEmail = novoEmail;
    }

    public String getNovoEmail() {
        return novoEmail;
    }

    public void setNovoEmail(String novoEmail) {
        this.novoEmail = novoEmail;
    }
}

