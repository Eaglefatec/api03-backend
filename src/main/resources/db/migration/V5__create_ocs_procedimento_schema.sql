CREATE TABLE ocs (
    ocs_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ocs_nome varchar(100) NOT NULL DEFAULT 'OCS',
    ocs_inscricao_federal varchar(14) UNIQUE NOT NULL,
    ocs_endereco varchar(100) NOT NULL,
    ocs_endereco_numero varchar(100) NOT NULL,
    ocs_endereco_bairro varchar(100) NOT NULL,
    ocs_endereco_cidade varchar(100) NOT NULL,
    ocs_endereco_uf varchar(100) NOT NULL,
    ocs_endereco_cep varchar(8)   NOT NULL,
    ocs_contato_nome varchar(100) NOT NULL,
    ocs_contato_telefone varchar(100) NOT NULL
);

CREATE TABLE procedimento (
    proc_codigo_dgp VARCHAR(100) NOT NULL,
    proc_descricao VARCHAR(1000) NOT NULL,
    proc_quantidade INT NOT NULL DEFAULT 1,
    PRIMARY KEY (proc_codigo_dgp)
);
