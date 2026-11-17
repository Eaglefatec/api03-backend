CREATE TABLE guia_assinatura (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    numero_guia VARCHAR(100) NOT NULL,
    beneficiario_nome VARCHAR(255) NOT NULL,
    beneficiario_prec_cp VARCHAR(50) NOT NULL,
    beneficiario_email VARCHAR(255) NOT NULL,
    ocs_nome VARCHAR(255) NOT NULL,
    ocs_id BIGINT NULL,
    procedimentos_resumo TEXT NULL,
    nome_arquivo_original VARCHAR(255) NOT NULL,
    caminho_arquivo_original VARCHAR(500) NOT NULL,
    caminho_arquivo_assinado VARCHAR(500) NULL,
    tamanho_bytes BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL DEFAULT 'AGUARDANDO_MEDICO_CHEFE',
    data_envio DATETIME NOT NULL,
    medico_chefe_usuario VARCHAR(100) NULL,
    data_assinatura_medico DATETIME NULL,
    assinatura_medico_base64 LONGTEXT NULL,
    chefe_secao_usuario VARCHAR(100) NULL,
    data_assinatura_secao DATETIME NULL,
    assinatura_secao_base64 LONGTEXT NULL,
    data_validade DATE NULL,
    data_envio_email DATETIME NULL,
    status_email VARCHAR(30) NULL,
    motivo_rejeicao VARCHAR(500) NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT uk_guia_assinatura_numero UNIQUE (numero_guia),
    FOREIGN KEY (ocs_id) REFERENCES ocs(ocs_id)
);

CREATE INDEX idx_guia_assinatura_status ON guia_assinatura(status);
CREATE INDEX idx_guia_assinatura_data_envio ON guia_assinatura(data_envio);

