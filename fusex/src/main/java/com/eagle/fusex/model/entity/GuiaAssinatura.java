package com.eagle.fusex.model.entity;

import com.eagle.fusex.model.enums.StatusEmail;
import com.eagle.fusex.model.enums.StatusEsteiraAssinatura;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "guia_assinatura")
public class GuiaAssinatura {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "numero_guia", nullable = false, unique = true, length = 100)
    private String numeroGuia;

    @Column(name = "beneficiario_nome", nullable = false)
    private String beneficiarioNome;

    @Column(name = "beneficiario_prec_cp", nullable = false, length = 50)
    private String beneficiarioPrecCp;

    @Column(name = "beneficiario_email", nullable = false)
    private String beneficiarioEmail;

    @Column(name = "ocs_nome", nullable = false)
    private String ocsNome;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ocs_id")
    private Ocs ocs;

    @Column(name = "procedimentos_resumo", columnDefinition = "TEXT")
    private String procedimentosResumo;

    @Column(name = "nome_arquivo_original", nullable = false)
    private String nomeArquivoOriginal;

    @Column(name = "caminho_arquivo_original", nullable = false, length = 500)
    private String caminhoArquivoOriginal;

    @Column(name = "caminho_arquivo_assinado", length = 500)
    private String caminhoArquivoAssinado;

    @Column(name = "tamanho_bytes", nullable = false)
    private Long tamanhoBytes;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 40)
    private StatusEsteiraAssinatura status = StatusEsteiraAssinatura.AGUARDANDO_MEDICO_CHEFE;

    @Column(name = "data_envio", nullable = false)
    private LocalDateTime dataEnvio;

    @Column(name = "medico_chefe_usuario", length = 100)
    private String medicoChefeUsuario;

    @Column(name = "data_assinatura_medico")
    private LocalDateTime dataAssinaturaMedico;

    @Column(name = "assinatura_medico_base64", columnDefinition = "LONGTEXT")
    private String assinaturaMedicoBase64;

    @Column(name = "chefe_secao_usuario", length = 100)
    private String chefeSecaoUsuario;

    @Column(name = "data_assinatura_secao")
    private LocalDateTime dataAssinaturaSecao;

    @Column(name = "assinatura_secao_base64", columnDefinition = "LONGTEXT")
    private String assinaturaSecaoBase64;

    @Column(name = "data_validade")
    private LocalDate dataValidade;

    @Column(name = "data_envio_email")
    private LocalDateTime dataEnvioEmail;

    @Enumerated(EnumType.STRING)
    @Column(name = "status_email", length = 30)
    private StatusEmail statusEmail;

    @Column(name = "motivo_rejeicao", length = 500)
    private String motivoRejeicao;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
        if (dataEnvio == null) {
            dataEnvio = LocalDateTime.now();
        }
    }

    public GuiaAssinatura() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNumeroGuia() {
        return numeroGuia;
    }

    public void setNumeroGuia(String numeroGuia) {
        this.numeroGuia = numeroGuia;
    }

    public String getBeneficiarioNome() {
        return beneficiarioNome;
    }

    public void setBeneficiarioNome(String beneficiarioNome) {
        this.beneficiarioNome = beneficiarioNome;
    }

    public String getBeneficiarioPrecCp() {
        return beneficiarioPrecCp;
    }

    public void setBeneficiarioPrecCp(String beneficiarioPrecCp) {
        this.beneficiarioPrecCp = beneficiarioPrecCp;
    }

    public String getBeneficiarioEmail() {
        return beneficiarioEmail;
    }

    public void setBeneficiarioEmail(String beneficiarioEmail) {
        this.beneficiarioEmail = beneficiarioEmail;
    }

    public String getOcsNome() {
        return ocsNome;
    }

    public void setOcsNome(String ocsNome) {
        this.ocsNome = ocsNome;
    }

    public Ocs getOcs() {
        return ocs;
    }

    public void setOcs(Ocs ocs) {
        this.ocs = ocs;
    }

    public String getProcedimentosResumo() {
        return procedimentosResumo;
    }

    public void setProcedimentosResumo(String procedimentosResumo) {
        this.procedimentosResumo = procedimentosResumo;
    }

    public String getNomeArquivoOriginal() {
        return nomeArquivoOriginal;
    }

    public void setNomeArquivoOriginal(String nomeArquivoOriginal) {
        this.nomeArquivoOriginal = nomeArquivoOriginal;
    }

    public String getCaminhoArquivoOriginal() {
        return caminhoArquivoOriginal;
    }

    public void setCaminhoArquivoOriginal(String caminhoArquivoOriginal) {
        this.caminhoArquivoOriginal = caminhoArquivoOriginal;
    }

    public String getCaminhoArquivoAssinado() {
        return caminhoArquivoAssinado;
    }

    public void setCaminhoArquivoAssinado(String caminhoArquivoAssinado) {
        this.caminhoArquivoAssinado = caminhoArquivoAssinado;
    }

    public Long getTamanhoBytes() {
        return tamanhoBytes;
    }

    public void setTamanhoBytes(Long tamanhoBytes) {
        this.tamanhoBytes = tamanhoBytes;
    }

    public StatusEsteiraAssinatura getStatus() {
        return status;
    }

    public void setStatus(StatusEsteiraAssinatura status) {
        this.status = status;
    }

    public LocalDateTime getDataEnvio() {
        return dataEnvio;
    }

    public void setDataEnvio(LocalDateTime dataEnvio) {
        this.dataEnvio = dataEnvio;
    }

    public String getMedicoChefeUsuario() {
        return medicoChefeUsuario;
    }

    public void setMedicoChefeUsuario(String medicoChefeUsuario) {
        this.medicoChefeUsuario = medicoChefeUsuario;
    }

    public LocalDateTime getDataAssinaturaMedico() {
        return dataAssinaturaMedico;
    }

    public void setDataAssinaturaMedico(LocalDateTime dataAssinaturaMedico) {
        this.dataAssinaturaMedico = dataAssinaturaMedico;
    }

    public String getAssinaturaMedicoBase64() {
        return assinaturaMedicoBase64;
    }

    public void setAssinaturaMedicoBase64(String assinaturaMedicoBase64) {
        this.assinaturaMedicoBase64 = assinaturaMedicoBase64;
    }

    public String getChefeSecaoUsuario() {
        return chefeSecaoUsuario;
    }

    public void setChefeSecaoUsuario(String chefeSecaoUsuario) {
        this.chefeSecaoUsuario = chefeSecaoUsuario;
    }

    public LocalDateTime getDataAssinaturaSecao() {
        return dataAssinaturaSecao;
    }

    public void setDataAssinaturaSecao(LocalDateTime dataAssinaturaSecao) {
        this.dataAssinaturaSecao = dataAssinaturaSecao;
    }

    public String getAssinaturaSecaoBase64() {
        return assinaturaSecaoBase64;
    }

    public void setAssinaturaSecaoBase64(String assinaturaSecaoBase64) {
        this.assinaturaSecaoBase64 = assinaturaSecaoBase64;
    }

    public LocalDate getDataValidade() {
        return dataValidade;
    }

    public void setDataValidade(LocalDate dataValidade) {
        this.dataValidade = dataValidade;
    }

    public LocalDateTime getDataEnvioEmail() {
        return dataEnvioEmail;
    }

    public void setDataEnvioEmail(LocalDateTime dataEnvioEmail) {
        this.dataEnvioEmail = dataEnvioEmail;
    }

    public StatusEmail getStatusEmail() {
        return statusEmail;
    }

    public void setStatusEmail(StatusEmail statusEmail) {
        this.statusEmail = statusEmail;
    }

    public String getMotivoRejeicao() {
        return motivoRejeicao;
    }

    public void setMotivoRejeicao(String motivoRejeicao) {
        this.motivoRejeicao = motivoRejeicao;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}

