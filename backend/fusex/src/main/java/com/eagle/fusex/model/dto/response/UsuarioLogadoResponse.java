package com.eagle.fusex.model.dto.response;

import java.util.List;

public class UsuarioLogadoResponse {
    private String username;
    private List<String> roles;
    private MedicoInfoDTO medico;

    public UsuarioLogadoResponse() {
    }

    public UsuarioLogadoResponse(String username, List<String> roles, MedicoInfoDTO medico) {
        this.username = username;
        this.roles = roles;
        this.medico = medico;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public MedicoInfoDTO getMedico() {
        return medico;
    }

    public void setMedico(MedicoInfoDTO medico) {
        this.medico = medico;
    }
}
