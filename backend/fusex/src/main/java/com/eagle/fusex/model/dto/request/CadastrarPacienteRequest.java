package com.eagle.fusex.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastrarPacienteRequest(
        @NotBlank(message = "O nome do paciente é obrigatório")
        String nome,

        @NotBlank(message = "A OM é obrigatória")
        @Size(max = 10, message = "A OM deve ter no máximo 10 caracteres")
        String om,

        @NotBlank(message = "O CPF/PREC é obrigatório")
        String cpfPrec,

        Integer idade,

        String telefone
) {
    public CadastrarPacienteRequest(String nome, String om, String cpfPrec) {
        this(nome, om, cpfPrec, null, null);
    }

    public String getNome() { return nome; }
    public String getOm() { return om; }
    public String getCpfPrec() { return cpfPrec; }
    public Integer getIdade() { return idade; }
    public String getTelefone() { return telefone; }
}
