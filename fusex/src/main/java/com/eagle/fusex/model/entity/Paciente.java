package com.eagle.fusex.model.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "paciente")
public class Paciente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nome;

    @Column(nullable = false, length = 14)
    private String cpf;

    @Column(nullable = false, length = 30)
    private String prec;

    @Column(nullable = false, length = 10)
    private String om;

    @Column(nullable = false, length = 255)
    private String email;

    @Column(nullable = true)
    private Integer idade;

    @Column(nullable = true, length = 20)
    private String telefone;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public Paciente() {
    }

    public Paciente(String nome, String cpf, String prec, String om, String email) {
        this(nome, cpf, prec, om, email, null, null);
    }

    public Paciente(String nome, String cpf, String prec, String om, String email, Integer idade, String telefone) {
        this.nome = nome;
        this.cpf = normalizarCpf(cpf);
        this.prec = prec != null ? prec.trim() : null;
        this.om = om;
        this.email = email != null ? email.trim().toLowerCase() : null;
        this.idade = idade;
        this.telefone = telefone;
    }

    // Construtores de compatibilidade com versões e testes legados
    public Paciente(String nome, String cpfPrec, String om) {
        this(nome, cpfPrec, cpfPrec, om, "paciente@fusex.eb.mil.br");
    }

    public Paciente(String nome, String cpfPrec, String om, Integer idade, String telefone) {
        this(nome, cpfPrec, cpfPrec, om, "paciente@fusex.eb.mil.br", idade, telefone);
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public static String normalizarCpf(String valor) {
        if (valor == null) return null;
        String limpo = valor.replaceAll("\\D", "");
        return limpo.isBlank() ? valor.trim() : limpo;
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

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = normalizarCpf(cpf);
    }

    public String getPrec() {
        return prec;
    }

    public void setPrec(String prec) {
        this.prec = prec != null ? prec.trim() : null;
    }

    public String getOm() {
        return om;
    }

    public void setOm(String om) {
        this.om = om;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email != null ? email.trim().toLowerCase() : null;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Helper de compatibilidade para exibição combinada
     */
    public String getCpfPrec() {
        if (cpf == null && prec == null) return null;
        if (cpf != null && prec != null) return cpf + " / " + prec;
        return cpf != null ? cpf : prec;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Paciente paciente)) return false;
        return Objects.equals(id, paciente.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
