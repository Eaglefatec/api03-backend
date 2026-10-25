CREATE TABLE tb_glosa (
                          id BIGINT PRIMARY KEY AUTO_INCREMENT,
                          guia_id BIGINT NOT NULL,
                          codigo_tuss VARCHAR(50) NULL,
                          justificativa_militar TEXT NOT NULL,
                          valor_glosa DECIMAL(10,2) NOT NULL,
                          data_registro DATETIME NOT NULL,
                          FOREIGN KEY (guia_id) REFERENCES guia(id)
);

CREATE INDEX idx_tb_glosa_guia ON tb_glosa(guia_id);