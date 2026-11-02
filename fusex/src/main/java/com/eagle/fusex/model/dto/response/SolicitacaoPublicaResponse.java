package com.eagle.fusex.model.dto.response;

import com.eagle.fusex.model.enums.Especialidade;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class SolicitacaoPublicaResponse {

    private String nomePaciente;
    private String om;
    private String cpfPrec;
    private Integer idade;
    private String telefone;
    private Set<Especialidade> especialidades;
    private String observacao;
    private Boolean triagemPreenchida;
    private List<ProcedimentoResumo> procedimentos = new ArrayList<>();

    public static class ProcedimentoResumo {
        private String codigoDgp;
        private String descricao;

        public ProcedimentoResumo() {
        }

        public ProcedimentoResumo(String codigoDgp, String descricao) {
            this.codigoDgp = codigoDgp;
            this.descricao = descricao;
        }

        public String getCodigoDgp() {
            return codigoDgp;
        }

        public void setCodigoDgp(String codigoDgp) {
            this.codigoDgp = codigoDgp;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }
    }

    public SolicitacaoPublicaResponse() {
    }

    public SolicitacaoPublicaResponse(String nomePaciente, String om, Set<Especialidade> especialidades,
                                      String observacao, Boolean triagemPreenchida) {
        this(nomePaciente, om, null, null, null, especialidades, observacao, triagemPreenchida, new ArrayList<>());
    }

    public SolicitacaoPublicaResponse(String nomePaciente, String om, Set<Especialidade> especialidades,
                                      String observacao, Boolean triagemPreenchida,
                                      List<ProcedimentoResumo> procedimentos) {
        this(nomePaciente, om, null, null, null, especialidades, observacao, triagemPreenchida, procedimentos);
    }

    public SolicitacaoPublicaResponse(String nomePaciente, String om, String cpfPrec, Integer idade, String telefone,
                                      Set<Especialidade> especialidades, String observacao, Boolean triagemPreenchida,
                                      List<ProcedimentoResumo> procedimentos) {
        this.nomePaciente = nomePaciente;
        this.om = om;
        this.cpfPrec = cpfPrec;
        this.idade = idade;
        this.telefone = telefone;
        this.especialidades = especialidades;
        this.observacao = observacao;
        this.triagemPreenchida = triagemPreenchida;
        this.procedimentos = procedimentos != null ? procedimentos : new ArrayList<>();
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

    public Boolean getTriagemPreenchida() {
        return triagemPreenchida;
    }

    public void setTriagemPreenchida(Boolean triagemPreenchida) {
        this.triagemPreenchida = triagemPreenchida;
    }

    public List<ProcedimentoResumo> getProcedimentos() {
        return procedimentos;
    }

    public void setProcedimentos(List<ProcedimentoResumo> procedimentos) {
        this.procedimentos = procedimentos != null ? procedimentos : new ArrayList<>();
    }
}
