ALTER TABLE paciente ADD COLUMN cpf VARCHAR(14) NULL;
ALTER TABLE paciente ADD COLUMN prec VARCHAR(30) NULL;
ALTER TABLE paciente ADD COLUMN email VARCHAR(255) NULL;

UPDATE paciente SET cpf = cpf_prec, prec = cpf_prec, email = 'paciente@fusex.eb.mil.br' WHERE cpf IS NULL;

ALTER TABLE paciente MODIFY COLUMN cpf VARCHAR(14) NOT NULL;
ALTER TABLE paciente MODIFY COLUMN prec VARCHAR(30) NOT NULL;
ALTER TABLE paciente MODIFY COLUMN email VARCHAR(255) NOT NULL;

CREATE INDEX idx_paciente_cpf ON paciente(cpf);
CREATE INDEX idx_paciente_prec ON paciente(prec);

ALTER TABLE paciente DROP COLUMN cpf_prec;

