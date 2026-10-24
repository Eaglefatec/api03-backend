package com.eagle.fusex.model.dto.response;

public class MedicoInfoDTO {
    private Long id;
    private String nome;
    private String crm;

    public MedicoInfoDTO() {
    }

    public MedicoInfoDTO(Long id, String nome, String crm) {
        this.id = id;
        this.nome = nome;
        this.crm = crm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCrm() {
        return crm;
    }

    public void setCrm(String crm) {
        this.crm = crm;
    }
}
