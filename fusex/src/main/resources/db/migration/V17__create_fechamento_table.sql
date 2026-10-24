CREATE TABLE fechamento (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ocs_id BIGINT NOT NULL,
    competencia_ano INT NOT NULL,
    competencia_mes INT NOT NULL,
    data_envio DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL,
    FOREIGN KEY (ocs_id) REFERENCES ocs(ocs_id)
);

CREATE INDEX idx_fechamento_ocs_competencia ON fechamento(ocs_id, competencia_ano, competencia_mes);

ALTER TABLE lote ADD COLUMN fechamento_id BIGINT NULL;
ALTER TABLE lote ADD CONSTRAINT fk_lote_fechamento FOREIGN KEY (fechamento_id) REFERENCES fechamento(id);
