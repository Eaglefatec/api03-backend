ALTER TABLE guia_assinatura ADD COLUMN solicitacao_id BIGINT NULL;
ALTER TABLE guia_assinatura ADD CONSTRAINT fk_guia_assinatura_solicitacao FOREIGN KEY (solicitacao_id) REFERENCES solicitacao_medica(id);
CREATE INDEX idx_guia_assinatura_solicitacao_id ON guia_assinatura(solicitacao_id);
