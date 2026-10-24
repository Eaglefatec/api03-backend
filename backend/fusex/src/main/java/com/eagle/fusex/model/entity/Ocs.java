package com.eagle.fusex.model.entity;

import com.eagle.fusex.model.enums.Especialidade;
import com.eagle.fusex.model.enums.TipoOcs;
import jakarta.persistence.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

@Entity
@Table(name = "ocs")
public class Ocs {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ocs_id")
    private Long ocsId;

    @Column(name = "ocs_nome", nullable = false, unique = true)
    private String ocsNome = "OCS";

    @Enumerated(EnumType.STRING)
    @Column(name = "ocs_tipo", nullable = false)
    private TipoOcs ocsTipo = TipoOcs.OCS;

    @Column(name = "ocs_contrato_numero")
    private String ocsContratoNumero;

    @Column(name = "ocs_inicio_vigencia")
    private LocalDate ocsInicioVigencia;

    @Column(name = "ocs_termino_vigencia")
    private LocalDate ocsTerminoVigencia;

    @Column(name = "ocs_inscricao_federal", unique = true)
    private String ocsInscricaoFederal;

    @Column(name = "ocs_endereco")
    private String ocsEndereco;

    @Column(name = "ocs_endereco_numero")
    private String ocsEnderecoNumero;

    @Column(name = "ocs_endereco_bairro")
    private String ocsEnderecoBairro;

    @Column(name = "ocs_endereco_cidade")
    private String ocsEnderecoCidade;

    @Column(name = "ocs_endereco_uf")
    private String ocsEnderecoUf;

    @Column(name = "ocs_endereco_cep")
    private String ocsEnderecoCep;

    @Column(name = "ocs_contato_nome")
    private String ocsContatoNome;

    @Column(name = "ocs_contato_telefone")
    private String ocsContatoTelefone;

    @Column(name = "login_usuario", unique = true)
    private String loginUsuario;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ocs_especialidade_item", joinColumns = @JoinColumn(name = "ocs_id"))
    @Column(name = "especialidade")
    @Enumerated(EnumType.STRING)
    private Set<Especialidade> especialidades = new HashSet<>();

    @OneToMany(mappedBy = "ocs")
    private Set<OcsProcedimento> ocsProcedimentos = new HashSet<>();

    public Ocs() {
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

    public TipoOcs getOcsTipo() {
        return ocsTipo;
    }

    public void setOcsTipo(TipoOcs ocsTipo) {
        this.ocsTipo = ocsTipo;
    }

    public String getOcsContratoNumero() {
        return ocsContratoNumero;
    }

    public void setOcsContratoNumero(String ocsContratoNumero) {
        this.ocsContratoNumero = ocsContratoNumero;
    }

    public LocalDate getOcsInicioVigencia() {
        return ocsInicioVigencia;
    }

    public void setOcsInicioVigencia(LocalDate ocsInicioVigencia) {
        this.ocsInicioVigencia = ocsInicioVigencia;
    }

    public LocalDate getOcsTerminoVigencia() {
        return ocsTerminoVigencia;
    }

    public void setOcsTerminoVigencia(LocalDate ocsTerminoVigencia) {
        this.ocsTerminoVigencia = ocsTerminoVigencia;
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

    public String getLoginUsuario() {
        return loginUsuario;
    }

    public void setLoginUsuario(String loginUsuario) {
        this.loginUsuario = loginUsuario;
    }

    public Set<Especialidade> getEspecialidades() {
        return especialidades;
    }

    public void setEspecialidades(Set<Especialidade> especialidades) {
        this.especialidades = especialidades;
    }

    public Set<OcsProcedimento> getOcsProcedimentos() {
        return ocsProcedimentos;
    }

    public void setOcsProcedimentos(Set<OcsProcedimento> ocsProcedimentos) {
        this.ocsProcedimentos = ocsProcedimentos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Ocs ocs)) return false;
        return Objects.equals(ocsId, ocs.ocsId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ocsId);
    }
}
