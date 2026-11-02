package com.eagle.fusex.model.dto.response;

public record PacienteResponse(
        Long id,
        String nome,
        String om,
        String cpf,
        String prec,
        String email,
        Integer idade,
        String telefone
) {
    public PacienteResponse(Long id, String nome, String om, String cpf, String prec, String email) {
        this(id, nome, om, cpf, prec, email, null, null);
    }

    public Long getId() { return id; }
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
