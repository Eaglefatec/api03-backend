package com.eagle.fusex.model.dto.request;

public record CadastroPacienteConfirmacaoDTO(
        boolean confirmarCadastro,
        String nome,
        String cpf,
        String prec,
        String om,
        String email,
        String telefone,
        Integer idade
) {
}

