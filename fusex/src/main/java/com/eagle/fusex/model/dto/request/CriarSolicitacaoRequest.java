package com.eagle.fusex.model.dto.request;

import com.eagle.fusex.model.enums.Especialidade;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.Set;

public class CriarSolicitacaoRequest {

    @Positive(message = "medicoId deve ser positivo")
    private Long medicoId;

    @Positive(message = "medicoResponsavelId deve ser positivo")
    private Long medicoResponsavelId;

    @NotBlank(message = "nomePaciente é obrigatório")
    @Size(max = 255, message = "nomePaciente deve ter no máximo 255 caracteres")
    private String nomePaciente;

    @NotBlank(message = "om é obrigatório")
    @Size(max = 10, message = "om deve ter no máximo 10 caracteres")
    private String om;

    @NotBlank(message = "cpfPrec é obrigatório")
    @Size(max = 20, message = "cpfPrec deve ter no máximo 20 caracteres")
    private String cpfPrec;

    @NotEmpty(message = "especialidades não pode estar vazio")
    private Set<Especialidade> especialidades;

    @Size(max = 500, message = "observacao deve ter no máximo 500 caracteres")
    private String observacao;

    @Valid
    @NotEmpty(message = "procedimentos não pode estar vazio")
    private List<ProcedimentoQuantidade> procedimentos;

    public CriarSolicitacaoRequest() {
    }

    public static class ProcedimentoQuantidade {

        @NotBlank(message = "procedimentoCodigoDgp é obrigatório")
        private String procedimentoCodigoDgp;

        @NotNull(message = "quantidade é obrigatória")
        @Positive(message = "quantidade deve ser positiva")
        private Integer quantidade;

        public ProcedimentoQuantidade() {
        }

        public ProcedimentoQuantidade(String procedimentoCodigoDgp, Integer quantidade) {
            this.procedimentoCodigoDgp = procedimentoCodigoDgp;
            this.quantidade = quantidade;
        }

        public String getProcedimentoCodigoDgp() {
            return procedimentoCodigoDgp;
        }

        public void setProcedimentoCodigoDgp(String procedimentoCodigoDgp) {
            this.procedimentoCodigoDgp = procedimentoCodigoDgp;
        }

        public Integer getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(Integer quantidade) {
            this.quantidade = quantidade;
        }
    }

    public Long getMedicoId() {
        return medicoId;
    }

    public void setMedicoId(Long medicoId) {
        this.medicoId = medicoId;
    }

    public Long getMedicoResponsavelId() {
        return medicoResponsavelId;
    }

    public void setMedicoResponsavelId(Long medicoResponsavelId) {
        this.medicoResponsavelId = medicoResponsavelId;
    }

    public String getNomePaciente() {
        return nomePaciente;
    }

    public void setNomePaciente(String nomePaciente) {
        this.nomePaciente = nomePaciente;
    }

    public String getOm() {
        return om;
    }

    public void setOm(String om) {
        this.om = om;
    }

    public String getCpfPrec() {
        return cpfPrec;
    }

    public void setCpfPrec(String cpfPrec) {
        this.cpfPrec = cpfPrec;
    }

    public Set<Especialidade> getEspecialidades() {
        return especialidades;
    }

    public void setEspecialidades(Set<Especialidade> especialidades) {
        this.especialidades = especialidades;
    }

    public String getObservacao() {
        return observacao;
    }

    public void setObservacao(String observacao) {
        this.observacao = observacao;
    }

    public List<ProcedimentoQuantidade> getProcedimentos() {
        return procedimentos;
    }

    public void setProcedimentos(List<ProcedimentoQuantidade> procedimentos) {
        this.procedimentos = procedimentos;
    }
}
