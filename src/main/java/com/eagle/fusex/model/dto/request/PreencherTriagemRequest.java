package com.eagle.fusex.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public class PreencherTriagemRequest {

    @NotBlank(message = "cpfPrec é obrigatório")
    @Size(max = 20, message = "cpfPrec deve ter no máximo 20 caracteres")
    private String cpfPrec;

    @NotNull(message = "idade é obrigatória")
    @Positive(message = "idade deve ser um número positivo")
    private Integer idade;

    @NotBlank(message = "telefone é obrigatório")
    @Size(max = 20, message = "telefone deve ter no máximo 20 caracteres")
    private String telefone;

    @NotNull(message = "ocsId é obrigatório")
    @Positive(message = "ocsId deve ser positivo")
    private Long ocsId;

    @NotNull(message = "aceitoTermos é obrigatório")
    private Boolean aceitoTermos;

    public PreencherTriagemRequest() {
    }

    public PreencherTriagemRequest(String cpfPrec, Integer idade, String telefone, Long ocsId, Boolean aceitoTermos) {
        this.cpfPrec = cpfPrec;
        this.idade = idade;
        this.telefone = telefone;
        this.ocsId = ocsId;
        this.aceitoTermos = aceitoTermos;
    }

    public String getCpfPrec() {
        return cpfPrec;
    }

    public void setCpfPrec(String cpfPrec) {
        this.cpfPrec = cpfPrec;
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
