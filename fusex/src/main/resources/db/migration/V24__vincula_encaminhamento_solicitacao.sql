ALTER TABLE encaminhamento ADD COLUMN solicitacao_id BIGINT NULL;
ALTER TABLE encaminhamento ADD CONSTRAINT fk_encaminhamento_solicitacao FOREIGN KEY (solicitacao_id) REFERENCES solicitacao_medica(id);
CREATE INDEX idx_encaminhamento_solicitacao_id ON encaminhamento(solicitacao_id);
