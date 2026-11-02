package com.eagle.fusex.model.dto;

import com.eagle.fusex.model.enums.StatusEmail;

public record ResultadoEnvioEmail(
        boolean sucesso,
        StatusEmail status,
        String mensagem,
        boolean retentavel
) {
    public static ResultadoEnvioEmail sucesso(String destinatario) {
        return new ResultadoEnvioEmail(true, StatusEmail.ENVIADO, "E-mail enviado com sucesso para: " + destinatario, false);
    }

    public static ResultadoEnvioEmail simulado(String destinatario) {
        return new ResultadoEnvioEmail(false, StatusEmail.SIMULADO, "Modo de simulação: envio desativado no ambiente local para: " + destinatario, false);
    }

    public static ResultadoEnvioEmail falhaInvalido(String erro) {
        return new ResultadoEnvioEmail(false, StatusEmail.FALHA_ENVIO, erro, false);
    }

    public static ResultadoEnvioEmail falhaConexao(String erro) {
        return new ResultadoEnvioEmail(false, StatusEmail.PENDENTE_REENVIO, erro, true);
    }
}

