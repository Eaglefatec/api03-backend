package com.eagle.fusex.model.dto.response;

import java.util.List;

public class TriagemResponse {

    public static class Opcao {
        private String tipo;
        private String descricao;

        public Opcao() {
        }

        public Opcao(String tipo, String descricao) {
            this.tipo = tipo;
            this.descricao = descricao;
        }

        public String getTipo() {
            return tipo;
        }

        public void setTipo(String tipo) {
            this.tipo = tipo;
        }

        public String getDescricao() {
            return descricao;
        }

        public void setDescricao(String descricao) {
            this.descricao = descricao;
        }
    }

    private String mensagem;
    private List<Opcao> opcoes;

    public TriagemResponse() {
    }

    public TriagemResponse(String mensagem, List<Opcao> opcoes) {
        this.mensagem = mensagem;
        this.opcoes = opcoes;
    }

    public String getMensagem() {
        return mensagem;
    }

    public void setMensagem(String mensagem) {
        this.mensagem = mensagem;
    }

    public List<Opcao> getOpcoes() {
        return opcoes;
    }

    public void setOpcoes(List<Opcao> opcoes) {
        this.opcoes = opcoes;
    }
}
