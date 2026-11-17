package com.eagle.fusex.model.dto.response;

import com.eagle.fusex.model.enums.StatusEmail;
import com.eagle.fusex.model.enums.StatusEsteiraAssinatura;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record GuiaAssinaturaResponse(
        Long id,
        String numeroGuia,
        String beneficiarioNome,
        String beneficiarioPrecCp,
        String beneficiarioEmail,
        String ocsNome,
        String nomeArquivoOriginal,
        Long tamanhoBytes,
        StatusEsteiraAssinatura status,
        String statusCodigo,
        String statusDescricao,
        LocalDateTime dataEnvio,
        LocalDateTime dataAssinaturaMedico,
        LocalDateTime dataAssinaturaSecao,
        LocalDate dataValidade,
        LocalDateTime dataEnvioEmail,
        StatusEmail statusEmail,
        String mensagemPronta,
        String mensagem
) {
}

