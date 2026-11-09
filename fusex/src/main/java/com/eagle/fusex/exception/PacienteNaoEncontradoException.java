package com.eagle.fusex.exception;

import com.eagle.fusex.service.GuiaAssinaturaPdfService.DadosExtraidosPdf;

public class PacienteNaoEncontradoException extends RuntimeException {

    private final DadosExtraidosPdf dadosExtraidos;

    public PacienteNaoEncontradoException(String message, DadosExtraidosPdf dadosExtraidos) {
        super(message);
        this.dadosExtraidos = dadosExtraidos;
    }

    public DadosExtraidosPdf getDadosExtraidos() {
        return dadosExtraidos;
    }
}

