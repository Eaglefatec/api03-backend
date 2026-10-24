ALTER TABLE ocs ADD COLUMN login_usuario VARCHAR(50) NULL;
ALTER TABLE ocs ADD CONSTRAINT uk_ocs_login_usuario UNIQUE (login_usuario);
