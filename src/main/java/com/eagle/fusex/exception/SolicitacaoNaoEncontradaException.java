package com.eagle.fusex.exception;

public class SolicitacaoNaoEncontradaException extends ResourceNotFoundException {
    public SolicitacaoNaoEncontradaException(String message) {
        super(message);
    }
}
