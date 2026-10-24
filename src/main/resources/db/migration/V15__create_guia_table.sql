CREATE TABLE guia (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ocs_id BIGINT NOT NULL,
    numero_guia VARCHAR(100) NOT NULL,
    beneficiario_prec_cp VARCHAR(50) NOT NULL,
    data_atendimento DATE NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    caminho_arquivo VARCHAR(500) NOT NULL,
    tipo_arquivo VARCHAR(100) NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME,
    CONSTRAINT uk_guia_numero_guia UNIQUE (numero_guia),
    FOREIGN KEY (ocs_id) REFERENCES ocs(ocs_id)
);

CREATE INDEX idx_guia_ocs ON guia(ocs_id);
