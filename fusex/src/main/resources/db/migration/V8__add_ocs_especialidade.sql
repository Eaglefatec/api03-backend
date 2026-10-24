DELETE FROM ocs WHERE ocs_nome = 'Fisio Vip Clin';

ALTER TABLE ocs
    ADD COLUMN ocs_especialidade VARCHAR(50) NOT NULL;