package com.eagle.fusex.model.dto.response;

import com.eagle.fusex.model.enums.StatusEmail;

public class SolicitacaoResponse {

    private Long id;
    private String tokenPublico;
    private String linkBeneficiario;
    private String mensagem;
    private Boolean emailEnviado;
    private String qrCodeUrl;
    private StatusEmail statusEmail;
    private String emailDestinatario;
    private String emailMensagem;

    public SolicitacaoResponse() {
    }

    public SolicitacaoResponse(Long id, String tokenPublico, String linkBeneficiario, String mensagem) {
        this(id, tokenPublico, linkBeneficiario, mensagem, null, null);
    }

    public SolicitacaoResponse(Long id, String tokenPublico, String linkBeneficiario, String mensagem,
                                Boolean emailEnviado, String qrCodeUrl) {
        this(id, tokenPublico, linkBeneficiario, mensagem, emailEnviado, qrCodeUrl, null, null, null);
    }

    public SolicitacaoResponse(Long id, String tokenPublico, String linkBeneficiario, String mensagem,
                                Boolean emailEnviado, String qrCodeUrl, StatusEmail statusEmail,
                                String emailDestinatario, String emailMensagem) {
        this.id = id;
        this.tokenPublico = tokenPublico;
        this.linkBeneficiario = linkBeneficiario;
        this.mensagem = mensagem;
        this.emailEnviado = emailEnviado;
        this.qrCodeUrl = qrCodeUrl;
        this.statusEmail = statusEmail;
        this.emailDestinatario = emailDestinatario;
        this.emailMensagem = emailMensagem;
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

    public Boolean getEmailEnviado() {
        return emailEnviado;
    }

    public void setEmailEnviado(Boolean emailEnviado) {
        this.emailEnviado = emailEnviado;
    }

    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public StatusEmail getStatusEmail() {
        return statusEmail;
    }

    public void setStatusEmail(StatusEmail statusEmail) {
        this.statusEmail = statusEmail;
    }

    public String getEmailDestinatario() {
        return emailDestinatario;
    }

    public void setEmailDestinatario(String emailDestinatario) {
        this.emailDestinatario = emailDestinatario;
    }

    public String getEmailMensagem() {
        return emailMensagem;
    }

    public void setEmailMensagem(String emailMensagem) {
        this.emailMensagem = emailMensagem;
    }
}
