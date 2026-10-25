package com.eagle.fusex.model.entity;

import com.eagle.fusex.model.enums.StatusGuia;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Table(name = "guia")
public class Guia {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ocs_id", nullable = false)
    private Ocs ocs;

    @Column(name = "numero_guia", nullable = false, unique = true)
    private String numeroGuia;

    @Column(name = "beneficiario_prec_cp", nullable = false)
    private String beneficiarioPrecCp;

    @Column(name = "beneficiario_nome")
    private String beneficiarioNome;

    @Column(name = "codigo_tuss")
    private String codigoTuss;

    @Column(name = "valor_contratado", nullable = false)
    private BigDecimal valorContratado = BigDecimal.ZERO;

    @Column(name = "valor_faturado", nullable = false)
    private BigDecimal valorFaturado = BigDecimal.ZERO;

    @Column(name = "data_atendimento", nullable = false)
    private LocalDate dataAtendimento;

    @Column(name = "nome_arquivo", nullable = false)
    private String nomeArquivo;

    @Column(name = "caminho_arquivo", nullable = false)
    private String caminhoArquivo;

    @Column(name = "tipo_arquivo", nullable = false)
    private String tipoArquivo;

    @Column(name = "tamanho_bytes", nullable = false)
    private Long tamanhoBytes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private StatusGuia status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }

    public Guia() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Ocs getOcs() {
        return ocs;
    }

    public void setOcs(Ocs ocs) {
        this.ocs = ocs;
    }

    public String getNumeroGuia() {
        return numeroGuia;
    }

    public void setNumeroGuia(String numeroGuia) {
        this.numeroGuia = numeroGuia;
    }

    public String getBeneficiarioPrecCp() {
        return beneficiarioPrecCp;
    }

    public void setBeneficiarioPrecCp(String beneficiarioPrecCp) {
        this.beneficiarioPrecCp = beneficiarioPrecCp;
    }

    public String getBeneficiarioNome() {
        return beneficiarioNome;
    }

    public void setBeneficiarioNome(String beneficiarioNome) {
        this.beneficiarioNome = beneficiarioNome;
    }

    public String getCodigoTuss() {
        return codigoTuss;
    }

    public void setCodigoTuss(String codigoTuss) {
        this.codigoTuss = codigoTuss;
    }

    public BigDecimal getValorContratado() {
        return valorContratado;
    }

    public void setValorContratado(BigDecimal valorContratado) {
        this.valorContratado = valorContratado;
    }

    public BigDecimal getValorFaturado() {
        return valorFaturado;
    }

    public void setValorFaturado(BigDecimal valorFaturado) {
        this.valorFaturado = valorFaturado;
    }

    public LocalDate getDataAtendimento() {
        return dataAtendimento;
    }

    public void setDataAtendimento(LocalDate dataAtendimento) {
        this.dataAtendimento = dataAtendimento;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public void setNomeArquivo(String nomeArquivo) {
        this.nomeArquivo = nomeArquivo;
    }

    public String getCaminhoArquivo() {
        return caminhoArquivo;
    }

    public void setCaminhoArquivo(String caminhoArquivo) {
        this.caminhoArquivo = caminhoArquivo;
    }

    public String getTipoArquivo() {
        return tipoArquivo;
    }

    public void setTipoArquivo(String tipoArquivo) {
        this.tipoArquivo = tipoArquivo;
    }

    public Long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public void setTamanhoBytes(Long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }

    public StatusGuia getStatus() {
        return status;
    }

    public void setStatus(StatusGuia status) {
        this.status = status;
    }

    public Lote getLote() {
        return lote;
    }

    public void setLote(Lote lote) {
        this.lote = lote;
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
        if (!(o instanceof Guia guia)) return false;
        return Objects.equals(id, guia.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}