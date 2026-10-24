package com.eagle.fusex.model.dto.response;

public record PacienteResponse(
        Long id,
        String nome,
        String om,
        String cpfPrec,
        Integer idade,
        String telefone
) {
    public PacienteResponse(Long id, String nome, String om, String cpfPrec) {
        this(id, nome, om, cpfPrec, null, null);
    }

    public Long getId() { return id; }
    public String getNome() { return nome; }
    public String getOm() { return om; }
    public String getCpfPrec() { return cpfPrec; }
    public Integer getIdade() { return idade; }
    public String getTelefone() { return telefone; }
}
