package com.eagle.fusex.model.dto.response;

public class SolicitacaoResponse {

    private Long id;
    private String tokenPublico;
    private String linkBeneficiario;
    private String mensagem;

    public SolicitacaoResponse() {
    }

    public SolicitacaoResponse(Long id, String tokenPublico, String linkBeneficiario, String mensagem) {
        this.id = id;
        this.tokenPublico = tokenPublico;
        this.linkBeneficiario = linkBeneficiario;
        this.mensagem = mensagem;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTokenPublico() {
        return tokenPublico;
    }

    public void setTokenPublico(String tokenPublico) {
        this.tokenPublico = tokenPublico;
    }

    public String getLinkBeneficiario() {
        return linkBeneficiario;
    }

    public void setLinkBeneficiario(String linkBeneficiario) {
        this.linkBeneficiario = linkBeneficiario;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }
}
