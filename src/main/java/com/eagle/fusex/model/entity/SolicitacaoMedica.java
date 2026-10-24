package com.eagle.fusex.model.entity;

import com.eagle.fusex.model.enums.Especialidade;
import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "solicitacao_medica")
public class SolicitacaoMedica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_id", nullable = false)
    private Medico medico;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "paciente_id", nullable = false)
    private Paciente paciente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medico_responsavel_id", nullable = true)
    private Medico medicoResponsavel;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "solicitacao_especialidade", joinColumns = @JoinColumn(name = "solicitacao_id"))
    @Column(name = "especialidade")
    @Enumerated(EnumType.STRING)
    private Set<Especialidade> especialidades = new HashSet<>();

    @Column(length = 500)
    private String observacao;

    @Column(nullable = false, unique = true)
    private String tokenPublico;

    @Column(nullable = false)
    private Boolean valida = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCriacao;

    @Column(name = "numero_guia", unique = true)
    private String numeroGuia;

    @Column(name = "data_importacao_guia")
    private LocalDateTime dataImportacaoGuia;

    @Column(name = "data_validade")
    private LocalDate dataValidade;

    @Column(name = "caminho_arquivo_guia", length = 500)
    private String caminhoArquivoGuia;

    public SolicitacaoMedica() {
    }

    @PrePersist
    protected void onCreate() {
        dataCriacao = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Medico getMedico() {
        return medico;
    }

    public void setMedico(Medico medico) {
        this.medico = medico;
    }

    public Paciente getPaciente() {
        return paciente;
    }

    public void setPaciente(Paciente paciente) {
        this.paciente = paciente;
    }

    public Medico getMedicoResponsavel() {
        return medicoResponsavel;
    }

    public void setMedicoResponsavel(Medico medicoResponsavel) {
        this.medicoResponsavel = medicoResponsavel;
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

    public String getTokenPublico() {
        return tokenPublico;
    }

    public void setTokenPublico(String tokenPublico) {
        this.tokenPublico = tokenPublico;
    }

    public Boolean getValida() {
        return valida;
    }

    public void setValida(Boolean valida) {
        this.valida = valida;
    }

    public LocalDateTime getDataCriacao() {
        return dataCriacao;
    }

    public void setDataCriacao(LocalDateTime dataCriacao) {
        this.dataCriacao = dataCriacao;
    }

    public String getNumeroGuia() {
        return numeroGuia;
    }

    public void setNumeroGuia(String numeroGuia) {
        this.numeroGuia = numeroGuia;
    }

    public LocalDateTime getDataImportacaoGuia() {
        return dataImportacaoGuia;
    }

    public void setDataImportacaoGuia(LocalDateTime dataImportacaoGuia) {
        this.dataImportacaoGuia = dataImportacaoGuia;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public String getCaminhoArquivoGuia() {
        return caminhoArquivoGuia;
    }

    public void setCaminhoArquivoGuia(String caminhoArquivoGuia) {
        this.caminhoArquivoGuia = caminhoArquivoGuia;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SolicitacaoMedica that)) return false;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}
