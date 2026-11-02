package com.eagle.fusex.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CadastrarPacienteRequest(
        @NotBlank(message = "O nome do paciente é obrigatório")
        String nome,

        @NotBlank(message = "A OM é obrigatória")
        @Size(max = 10, message = "A OM deve ter no máximo 10 caracteres")
        String om,

        @NotBlank(message = "O CPF é obrigatório")
        @Size(max = 14, message = "O CPF deve ter no máximo 14 caracteres")
        String cpf,

        @NotBlank(message = "O PREC é obrigatório")
        @Size(max = 30, message = "O PREC deve ter no máximo 30 caracteres")
        String prec,

        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "O e-mail informado é inválido")
        String email,

        Integer idade,

        String telefone
) {
    public CadastrarPacienteRequest(String nome, String om, String cpf, String prec, String email) {
        this(nome, om, cpf, prec, email, null, null);
    }

    public String getNome() { return nome; }
    public String getOm() { return om; }
    public String getCpf() { return cpf; }
    public String getPrec() { return prec; }
    public String getEmail() { return email; }
    public Integer getIdade() { return idade; }
    public String getTelefone() { return telefone; }

    public String getCpfPrec() {
        if (cpf == null && prec == null) return null;
        if (cpf != null && prec != null) return cpf + " / " + prec;
        return cpf != null ? cpf : prec;
    }
}
