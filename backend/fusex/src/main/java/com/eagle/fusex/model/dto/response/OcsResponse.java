package com.eagle.fusex.model.dto.response;

import com.eagle.fusex.model.entity.Ocs;

import java.math.BigDecimal;

public class OcsResponse {

    private Long ocsId;
    private String ocsNome;
    private String ocsInscricaoFederal;
    private String ocsEndereco;
    private String ocsEnderecoNumero;
    private String ocsEnderecoBairro;
    private String ocsEnderecoCidade;
    private String ocsEnderecoUf;
    private String ocsEnderecoCep;
    private String ocsContatoNome;
    private String ocsContatoTelefone;
    private BigDecimal valor;
    private String tabelaReferencia;

    public OcsResponse() {
    }

    public OcsResponse(Long ocsId, String ocsNome, String ocsInscricaoFederal, String ocsEndereco,
                       String ocsEnderecoNumero, String ocsEnderecoBairro, String ocsEnderecoCidade,
                       String ocsEnderecoUf, String ocsEnderecoCep, String ocsContatoNome,
                       String ocsContatoTelefone) {
        this.ocsId = ocsId;
        this.ocsNome = ocsNome;
        this.ocsInscricaoFederal = ocsInscricaoFederal;
        this.ocsEndereco = ocsEndereco;
        this.ocsEnderecoNumero = ocsEnderecoNumero;
        this.ocsEnderecoBairro = ocsEnderecoBairro;
        this.ocsEnderecoCidade = ocsEnderecoCidade;
        this.ocsEnderecoUf = ocsEnderecoUf;
        this.ocsEnderecoCep = ocsEnderecoCep;
        this.ocsContatoNome = ocsContatoNome;
        this.ocsContatoTelefone = ocsContatoTelefone;
    }

    public static OcsResponse from(Ocs ocs) {
        if (ocs == null) {
            return null;
        }
        return new OcsResponse(
                ocs.getOcsId(), ocs.getOcsNome(), ocs.getOcsInscricaoFederal(),
                ocs.getOcsEndereco(), ocs.getOcsEnderecoNumero(), ocs.getOcsEnderecoBairro(),
                ocs.getOcsEnderecoCidade(), ocs.getOcsEnderecoUf(), ocs.getOcsEnderecoCep(),
                ocs.getOcsContatoNome(), ocs.getOcsContatoTelefone()
        );
    }

    public Long getOcsId() {
        return ocsId;
    }

    public void setOcsId(Long ocsId) {
        this.ocsId = ocsId;
    }

    public String getOcsNome() {
        return ocsNome;
    }

    public void setOcsNome(String ocsNome) {
        this.ocsNome = ocsNome;
    }

    public String getOcsInscricaoFederal() {
        return ocsInscricaoFederal;
    }

    public void setOcsInscricaoFederal(String ocsInscricaoFederal) {
        this.ocsInscricaoFederal = ocsInscricaoFederal;
    }

    public String getOcsEndereco() {
        return ocsEndereco;
    }

    public void setOcsEndereco(String ocsEndereco) {
        this.ocsEndereco = ocsEndereco;
    }

    public String getOcsEnderecoNumero() {
        return ocsEnderecoNumero;
    }

    public void setOcsEnderecoNumero(String ocsEnderecoNumero) {
        this.ocsEnderecoNumero = ocsEnderecoNumero;
    }

    public String getOcsEnderecoBairro() {
        return ocsEnderecoBairro;
    }

    public void setOcsEnderecoBairro(String ocsEnderecoBairro) {
        this.ocsEnderecoBairro = ocsEnderecoBairro;
    }

    public String getOcsEnderecoCidade() {
        return ocsEnderecoCidade;
    }

    public void setOcsEnderecoCidade(String ocsEnderecoCidade) {
        this.ocsEnderecoCidade = ocsEnderecoCidade;
    }

    public String getOcsEnderecoUf() {
        return ocsEnderecoUf;
    }

    public void setOcsEnderecoUf(String ocsEnderecoUf) {
        this.ocsEnderecoUf = ocsEnderecoUf;
    }

    public String getOcsEnderecoCep() {
        return ocsEnderecoCep;
    }

    public void setOcsEnderecoCep(String ocsEnderecoCep) {
        this.ocsEnderecoCep = ocsEnderecoCep;
    }

    public String getOcsContatoNome() {
        return ocsContatoNome;
    }

    public void setOcsContatoNome(String ocsContatoNome) {
        this.ocsContatoNome = ocsContatoNome;
    }

    public String getOcsContatoTelefone() {
        return ocsContatoTelefone;
    }

    public void setOcsContatoTelefone(String ocsContatoTelefone) {
        this.ocsContatoTelefone = ocsContatoTelefone;
    }

    public BigDecimal getValor() {
        return valor;
    }

    public void setValor(BigDecimal valor) {
        this.valor = valor;
    }

    public String getTabelaReferencia() {
        return tabelaReferencia;
    }

    public void setTabelaReferencia(String tabelaReferencia) {
        this.tabelaReferencia = tabelaReferencia;
    }
}
