CREATE TABLE lote (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ocs_id BIGINT NOT NULL,
    numero_lote INT NOT NULL,
    competencia_ano INT NOT NULL,
    competencia_mes INT NOT NULL,
    data_geracao DATETIME NOT NULL,
    fatura_nome_arquivo VARCHAR(255) NOT NULL,
    fatura_caminho_arquivo VARCHAR(500) NOT NULL,
    fatura_tipo_arquivo VARCHAR(100) NOT NULL,
    espelho_nome_arquivo VARCHAR(255) NOT NULL,
    espelho_caminho_arquivo VARCHAR(500) NOT NULL,
    espelho_tipo_arquivo VARCHAR(100) NOT NULL,
    FOREIGN KEY (ocs_id) REFERENCES ocs(ocs_id)
);

CREATE INDEX idx_lote_ocs_competencia ON lote(ocs_id, competencia_ano, competencia_mes);

ALTER TABLE guia ADD COLUMN lote_id BIGINT NULL;
ALTER TABLE guia ADD CONSTRAINT fk_guia_lote FOREIGN KEY (lote_id) REFERENCES lote(id);
