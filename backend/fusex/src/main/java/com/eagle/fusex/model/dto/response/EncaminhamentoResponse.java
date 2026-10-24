package com.eagle.fusex.model.dto.response;

import java.time.LocalDateTime;

public class EncaminhamentoResponse {
    private Long id;
    private LocalDateTime dataHora;
    private String mensagem;

    public EncaminhamentoResponse() {
    }

    public EncaminhamentoResponse(Long id, LocalDateTime dataHora, String mensagem) {
        this.id = id;
        this.dataHora = dataHora;
        this.mensagem = mensagem;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public void setDataHora(LocalDateTime dataHora) {
        this.dataHora = dataHora;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
