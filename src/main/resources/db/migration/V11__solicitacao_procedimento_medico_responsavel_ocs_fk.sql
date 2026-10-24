ALTER TABLE solicitacao_medica ADD COLUMN medico_responsavel_id BIGINT NULL;
ALTER TABLE solicitacao_medica
    ADD CONSTRAINT fk_solicitacao_medico_responsavel
    FOREIGN KEY (medico_responsavel_id) REFERENCES medico(id);

CREATE TABLE solicitacao_procedimento (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    solicitacao_id BIGINT NOT NULL,
    proc_codigo_dgp VARCHAR(100) NOT NULL,
    quantidade INT NOT NULL,
    valor_no_momento DECIMAL(10,2) NULL,
    FOREIGN KEY (solicitacao_id) REFERENCES solicitacao_medica(id),
    FOREIGN KEY (proc_codigo_dgp) REFERENCES procedimento(proc_codigo_dgp)
);

CREATE INDEX idx_solicitacao_procedimento_solicitacao ON solicitacao_procedimento(solicitacao_id);

ALTER TABLE triagem_pre_guia ADD COLUMN ocs_id BIGINT NULL;

UPDATE triagem_pre_guia t
    SET ocs_id = (SELECT o.ocs_id FROM ocs o WHERE o.ocs_nome = t.clinica_laboratorio LIMIT 1)
    WHERE t.ocs_id IS NULL;

ALTER TABLE triagem_pre_guia
    ADD CONSTRAINT fk_triagem_ocs FOREIGN KEY (ocs_id) REFERENCES ocs(ocs_id);

ALTER TABLE triagem_pre_guia MODIFY COLUMN ocs_id BIGINT NOT NULL;

ALTER TABLE triagem_pre_guia DROP COLUMN clinica_laboratorio;
