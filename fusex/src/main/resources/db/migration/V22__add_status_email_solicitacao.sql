ALTER TABLE solicitacao_medica ADD COLUMN status_email VARCHAR(30) NOT NULL DEFAULT 'SIMULADO';
ALTER TABLE solicitacao_medica ADD COLUMN email_destinatario VARCHAR(255) NULL;
ALTER TABLE solicitacao_medica ADD COLUMN email_tentativas INT NOT NULL DEFAULT 0;
ALTER TABLE solicitacao_medica ADD COLUMN email_ultimo_erro VARCHAR(500) NULL;
ALTER TABLE solicitacao_medica ADD COLUMN email_proxima_tentativa DATETIME NULL;

CREATE INDEX idx_solic_status_email ON solicitacao_medica(status_email);

