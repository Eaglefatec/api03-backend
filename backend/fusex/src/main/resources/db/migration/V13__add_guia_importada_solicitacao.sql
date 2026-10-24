ALTER TABLE solicitacao_medica ADD COLUMN numero_guia VARCHAR(100) NULL;
ALTER TABLE solicitacao_medica ADD COLUMN data_importacao_guia DATETIME NULL;
ALTER TABLE solicitacao_medica ADD COLUMN data_validade DATE NULL;
ALTER TABLE solicitacao_medica ADD COLUMN caminho_arquivo_guia VARCHAR(500) NULL;

ALTER TABLE solicitacao_medica ADD CONSTRAINT uk_solicitacao_numero_guia UNIQUE (numero_guia);
