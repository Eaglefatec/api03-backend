CREATE TABLE ocs_procedimento (
    ocs_id BIGINT NOT NULL,
    proc_codigo_dgp VARCHAR(100) NOT NULL,
    PRIMARY KEY (ocs_id, proc_codigo_dgp),
    FOREIGN KEY (ocs_id) REFERENCES ocs(ocs_id),
    FOREIGN KEY (proc_codigo_dgp) REFERENCES procedimento(proc_codigo_dgp)
);