CREATE TABLE paciente (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    nome VARCHAR(255) NOT NULL,
    cpf_prec VARCHAR(20) NOT NULL UNIQUE,
    om VARCHAR(10) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE solicitacao_medica (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    medico_id BIGINT NOT NULL,
    paciente_id BIGINT NOT NULL,
    observacao VARCHAR(500),
    token_publico VARCHAR(255) NOT NULL UNIQUE,
    valida BOOLEAN NOT NULL DEFAULT TRUE,
    data_criacao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (medico_id) REFERENCES medico(id),
    FOREIGN KEY (paciente_id) REFERENCES paciente(id)
);

CREATE TABLE solicitacao_especialidade (
    solicitacao_id BIGINT NOT NULL,
    especialidade VARCHAR(50) NOT NULL,
    FOREIGN KEY (solicitacao_id) REFERENCES solicitacao_medica(id),
    PRIMARY KEY (solicitacao_id, especialidade)
);

CREATE INDEX idx_solicitacao_token ON solicitacao_medica(token_publico);
CREATE INDEX idx_solicitacao_medico_paciente ON solicitacao_medica(medico_id, paciente_id);
