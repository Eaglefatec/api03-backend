CREATE TABLE triagem_pre_guia (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    solicitacao_id BIGINT NOT NULL UNIQUE,
    cpf_prec VARCHAR(20) NOT NULL,
    idade INT NOT NULL,
    telefone VARCHAR(20) NOT NULL,
    clinica_laboratorio VARCHAR(100) NOT NULL,
    aceito_termos BOOLEAN NOT NULL,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (solicitacao_id) REFERENCES solicitacao_medica(id)
);

CREATE INDEX idx_triagem_solicitacao ON triagem_pre_guia(solicitacao_id);
