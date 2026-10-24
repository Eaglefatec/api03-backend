package com.eagle.fusex.exception;

public class ResourceNotFoundException extends BusinessException {

    public ResourceNotFoundException(String message) {
        super(message);
    }

    public ResourceNotFoundException(String resource, Object identifier) {
        super(resource + " não encontrado(a) com identificador: " + identifier);
    }

    public ResourceNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
