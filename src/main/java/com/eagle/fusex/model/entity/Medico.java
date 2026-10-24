package com.eagle.fusex.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "medico")
public class Medico {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, unique = true)
    private String crm;

    @Column(nullable = false)
    private Boolean credenciado = false;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Medico() {}

    public Medico(String nome, String crm, Boolean credenciado) {
        this.nome = nome;
        this.crm = crm;
        this.credenciado = credenciado;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCrm() {
        return crm;
    }

    public void setCrm(String crm) {
        this.crm = crm;
    }

    public Boolean getCredenciado() {
        return credenciado;
    }

    public void setCredenciado(Boolean credenciado) {
        this.credenciado = credenciado;
    }

    public boolean isCredenciado() {
        return Boolean.TRUE.equals(credenciado);
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Medico medico)) return false;
        return Objects.equals(id, medico.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
