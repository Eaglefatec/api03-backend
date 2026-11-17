package com.eagle.fusex.model.dto.response;

import com.eagle.fusex.model.enums.StatusEsteiraAssinatura;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record GuiaAssinaturaDetalheResponse(
        Long id,
        String numeroGuia,
        String beneficiarioNome,
        String beneficiarioPrecCp,
        String beneficiarioEmail,
        String ocsNome,
        String nomeArquivoOriginal,
        StatusEsteiraAssinatura status,
        String statusCodigo,
        String statusDescricao,
        LocalDateTime dataEnvio,
        String medicoChefeUsuario,
        LocalDateTime dataAssinaturaMedico,
        String assinaturaMedicoBase64,
        String chefeSecaoUsuario,
        LocalDateTime dataAssinaturaSecao,
        String assinaturaSecaoBase64,
        LocalDate dataValidade,
        int validadeDias,
        String regraValidade,
        List<ProcedimentoGuiaDTO> procedimentos,
        String mensagemPronta,
        String pdfUrl
) {
}

