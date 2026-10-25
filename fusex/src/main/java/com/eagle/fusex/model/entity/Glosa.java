package com.eagle.fusex.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tb_glosa")
public class Glosa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guia_id", nullable = false)
    private Guia guia;

    @Column(name = "codigo_tuss")
    private String codigoTuss;

    @Column(name = "justificativa_militar", nullable = false, columnDefinition = "TEXT")
    private String justificativaMilitar;

    @Column(name = "valor_glosa", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorGlosa;

    @Column(name = "data_registro", nullable = false)
    private LocalDateTime dataRegistro = LocalDateTime.now();

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Guia getGuia() { return guia; }
    public void setGuia(Guia guia) { this.guia = guia; }

    public String getCodigoTuss() { return codigoTuss; }
    public void setCodigoTuss(String codigoTuss) { this.codigoTuss = codigoTuss; }

    public String getJustificativaMilitar() { return justificativaMilitar; }
    public void setJustificativaMilitar(String justificativaMilitar) { this.justificativaMilitar = justificativaMilitar; }

    public BigDecimal getValorGlosa() { return valorGlosa; }
    public void setValorGlosa(BigDecimal valorGlosa) { this.valorGlosa = valorGlosa; }

    public LocalDateTime getDataRegistro() { return dataRegistro; }
    public void setDataRegistro(LocalDateTime dataRegistro) { this.dataRegistro = dataRegistro; }
}