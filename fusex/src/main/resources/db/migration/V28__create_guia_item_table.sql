CREATE TABLE IF NOT EXISTS guia_item (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    guia_id BIGINT NOT NULL,
    codigo_tuss VARCHAR(20) NOT NULL,
    descricao_item VARCHAR(255) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    quantidade INT NOT NULL DEFAULT 1,
    valor_unitario DECIMAL(10,2) NOT NULL,
    data_item DATE NULL,
    FOREIGN KEY (guia_id) REFERENCES guia(id)
);
