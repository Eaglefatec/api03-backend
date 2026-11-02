package com.eagle.fusex.model.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public class PreGuiaConsolidadaResponse {

    private Beneficiario beneficiario;
    private OcsResponse ocs;
    private MedicoResumoResponse medicoEmissor;
    private MedicoResumoResponse medicoResponsavel;
    private List<ProcedimentoQuantidadeInfo> procedimentos;
    private InformacoesGerais informacoesGerais;

    public PreGuiaConsolidadaResponse() {
    }

    public PreGuiaConsolidadaResponse(Beneficiario beneficiario, OcsResponse ocs,
                                      MedicoResumoResponse medicoEmissor, MedicoResumoResponse medicoResponsavel,
                                      List<ProcedimentoQuantidadeInfo> procedimentos,
                                      InformacoesGerais informacoesGerais) {
        this.beneficiario = beneficiario;
        this.ocs = ocs;
        this.medicoEmissor = medicoEmissor;
        this.medicoResponsavel = medicoResponsavel;
        this.procedimentos = procedimentos;
        this.informacoesGerais = informacoesGerais;
    }

    public Beneficiario getBeneficiario() {
        return beneficiario;
    }

    public void setBeneficiario(Beneficiario beneficiario) {
        this.beneficiario = beneficiario;
    }

    public OcsResponse getOcs() {
        return ocs;
    }

    public void setOcs(OcsResponse ocs) {
        this.ocs = ocs;
    }

    public MedicoResumoResponse getMedicoEmissor() {
        return medicoEmissor;
    }

    public void setMedicoEmissor(MedicoResumoResponse medicoEmissor) {
        this.medicoEmissor = medicoEmissor;
    }

    public MedicoResumoResponse getMedicoResponsavel() {
        return medicoResponsavel;
    }

    public void setMedicoResponsavel(MedicoResumoResponse medicoResponsavel) {
        this.medicoResponsavel = medicoResponsavel;
    }

    public List<ProcedimentoQuantidadeInfo> getProcedimentos() {
        return procedimentos;
    }

    public void setProcedimentos(List<ProcedimentoQuantidadeInfo> procedimentos) {
        this.procedimentos = procedimentos;
    }

    public InformacoesGerais getInformacoesGerais() {
        return informacoesGerais;
    }

    public void setInformacoesGerais(InformacoesGerais informacoesGerais) {
        this.informacoesGerais = informacoesGerais;
    }

    public static class Beneficiario {
        private String nome;
        private Integer idade;
        private String cpf;
        private String prec;
        private String email;
        private String telefone;

        public Beneficiario() {
        }

        public Beneficiario(String nome, Integer idade, String cpf, String prec, String email, String telefone) {
            this.nome = nome;
            this.idade = idade;
            this.cpf = cpf;
            this.prec = prec;
            this.email = email;
            this.telefone = telefone;
        }

        public Beneficiario(String nome, Integer idade, String cpfPrec, String telefone) {
            this(nome, idade, cpfPrec, null, null, telefone);
        }

        public String getNome() {
            return nome;
        }

        public void setNome(String nome) {
            this.nome = nome;
        }

        public Integer getIdade() {
            return idade;
        }

        public void setIdade(Integer idade) {
            this.idade = idade;
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

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
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

        public String getTelefone() {
            return telefone;
        }

        public void setTelefone(String telefone) {
            this.telefone = telefone;
        }
    }

    public static class ProcedimentoQuantidadeInfo {
        private String procedimentoCodigoDgp;
        private String procedimentoDescricao;
        private Integer quantidade;
        private BigDecimal valor;

        public ProcedimentoQuantidadeInfo() {
        }

        public ProcedimentoQuantidadeInfo(String procedimentoCodigoDgp, String procedimentoDescricao, Integer quantidade) {
            this(procedimentoCodigoDgp, procedimentoDescricao, quantidade, null);
        }

        public ProcedimentoQuantidadeInfo(String procedimentoCodigoDgp, String procedimentoDescricao, Integer quantidade, BigDecimal valor) {
            this.procedimentoCodigoDgp = procedimentoCodigoDgp;
            this.procedimentoDescricao = procedimentoDescricao;
            this.quantidade = quantidade;
            this.valor = valor;
        }

        public String getProcedimentoCodigoDgp() {
            return procedimentoCodigoDgp;
        }

        public void setProcedimentoCodigoDgp(String procedimentoCodigoDgp) {
            this.procedimentoCodigoDgp = procedimentoCodigoDgp;
        }

        public String getProcedimentoDescricao() {
            return procedimentoDescricao;
        }

        public void setProcedimentoDescricao(String procedimentoDescricao) {
            this.procedimentoDescricao = procedimentoDescricao;
        }

        public Integer getQuantidade() {
            return quantidade;
        }

        public void setQuantidade(Integer quantidade) {
            this.quantidade = quantidade;
        }

        public BigDecimal getValor() {
            return valor;
        }

        public void setValor(BigDecimal valor) {
            this.valor = valor;
        }
    }

    public static class InformacoesGerais {
        private LocalDateTime data;
        private String unidadeGestoraOrigem;

        public InformacoesGerais() {
        }

        public InformacoesGerais(LocalDateTime data, String unidadeGestoraOrigem) {
            this.data = data;
            this.unidadeGestoraOrigem = unidadeGestoraOrigem;
        }

        public LocalDateTime getData() {
            return data;
        }

        public void setData(LocalDateTime data) {
            this.data = data;
        }

        public String getUnidadeGestoraOrigem() {
            return unidadeGestoraOrigem;
        }

        public void setUnidadeGestoraOrigem(String unidadeGestoraOrigem) {
            this.unidadeGestoraOrigem = unidadeGestoraOrigem;
        }
    }
}
