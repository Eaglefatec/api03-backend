package com.eagle.fusex.model.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "guia_item")
public class GuiaItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "guia_id", nullable = false)
    private Long guiaId;

    @Column(name = "codigo_tuss", nullable = false, length = 20)
    private String codigoTuss;

    @Column(name = "descricao_item", nullable = false, length = 255)
    private String descricaoItem;

    @Column(nullable = false, length = 50)
    private String tipo; // 'Procedimento' ou 'Insumo'

    @Column(nullable = false)
    private Integer quantidade = 1;

    @Column(name = "valor_unitario", nullable = false, precision = 10, scale = 2)
    private BigDecimal valorUnitario;

    @Column(name = "data_item")
    private LocalDate dataItem;

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getGuiaId() { return guiaId; }
    public void setGuiaId(Long guiaId) { this.guiaId = guiaId; }

    public String getCodigoTuss() { return codigoTuss; }
    public void setCodigoTuss(String codigoTuss) { this.codigoTuss = codigoTuss; }

    public String getDescricaoItem() { return descricaoItem; }
    public void setDescricaoItem(String descricaoItem) { this.descricaoItem = descricaoItem; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Integer getQuantidade() { return quantidade; }
    public void setQuantidade(Integer quantidade) { this.quantidade = quantidade; }

    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }

    public LocalDate getDataItem() { return dataItem; }
    public void setDataItem(LocalDate dataItem) { this.dataItem = dataItem; }
}