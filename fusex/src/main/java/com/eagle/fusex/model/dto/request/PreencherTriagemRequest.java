package com.eagle.fusex.model.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class PreencherTriagemRequest {

    @Size(max = 14, message = "cpf deve ter no máximo 14 caracteres")
    private String cpf;

    @Size(max = 30, message = "prec deve ter no máximo 30 caracteres")
    private String prec;

    @NotNull(message = "idade é obrigatória")
    @Positive(message = "idade deve ser um número positivo")
    private Integer idade;

    @Size(max = 20, message = "telefone deve ter no máximo 20 caracteres")
    private String telefone;

    @NotNull(message = "ocsId é obrigatório")
    @Positive(message = "ocsId deve ser positivo")
    private Long ocsId;

    @NotNull(message = "aceitoTermos é obrigatório")
    private Boolean aceitoTermos;

    public PreencherTriagemRequest() {
    }

    public PreencherTriagemRequest(String cpf, String prec, Integer idade, String telefone, Long ocsId, Boolean aceitoTermos) {
        this.cpf = cpf;
        this.prec = prec;
        this.idade = idade;
        this.telefone = telefone;
        this.ocsId = ocsId;
        this.aceitoTermos = aceitoTermos;
    }

    public PreencherTriagemRequest(String cpfPrec, Integer idade, String telefone, Long ocsId, Boolean aceitoTermos) {
        this(cpfPrec, null, idade, telefone, ocsId, aceitoTermos);
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getPrec() {
        return prec;
    }

    public void setPrec(String prec) {
        this.prec = prec;
    }

    public String getCpfPrec() {
        if (cpf == null && prec == null) return null;
        if (cpf != null && prec != null) return cpf + " / " + prec;
        return cpf != null ? cpf : prec;
    }

    public void setCpfPrec(String cpfPrec) {
        if (cpfPrec != null) {
            if (this.cpf == null) this.cpf = cpfPrec;
            if (this.prec == null) this.prec = cpfPrec;
        }
    }

    public Integer getIdade() {
        return idade;
    }

    public void setIdade(Integer idade) {
        this.idade = idade;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public Long getOcsId() {
        return ocsId;
    }

    public void setOcsId(Long ocsId) {
        this.ocsId = ocsId;
    }

    public Boolean getAceitoTermos() {
        return aceitoTermos;
    }

    public void setAceitoTermos(Boolean aceitoTermos) {
        this.aceitoTermos = aceitoTermos;
    }
}
