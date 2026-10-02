CREATE TABLE usuarios (
    id        BIGINT       NOT NULL AUTO_INCREMENT,
    nome      VARCHAR(255) NOT NULL,
    email     VARCHAR(255) NOT NULL,
    senha     VARCHAR(255) NOT NULL,
    tipo      VARCHAR(20)  NOT NULL,
    criado_em DATETIME(6)  NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_usuarios_email UNIQUE (email),
    CONSTRAINT ck_usuarios_tipo CHECK (tipo IN ('ALUNO', 'PROFESSOR', 'FUNCIONARIO'))
) ENGINE = InnoDB;

CREATE TABLE recursos (
    id           BIGINT       NOT NULL AUTO_INCREMENT,
    tipo_recurso VARCHAR(31)  NOT NULL,
    nome         VARCHAR(255) NOT NULL,
    localizacao  VARCHAR(255),
    ativo        BOOLEAN      NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB;

CREATE TABLE salas_estudo (
    recurso_id         BIGINT  NOT NULL,
    capacidade_pessoas INT     NOT NULL,
    possui_projetor    BOOLEAN NOT NULL,
    PRIMARY KEY (recurso_id),
    CONSTRAINT fk_salas_estudo_recurso FOREIGN KEY (recurso_id) REFERENCES recursos (id)
) ENGINE = InnoDB;

CREATE TABLE quadras (
    recurso_id BIGINT       NOT NULL,
    modalidade VARCHAR(255) NOT NULL,
    coberta    BOOLEAN      NOT NULL,
    PRIMARY KEY (recurso_id),
    CONSTRAINT fk_quadras_recurso FOREIGN KEY (recurso_id) REFERENCES recursos (id)
) ENGINE = InnoDB;

CREATE TABLE equipamentos_laboratorio (
    recurso_id        BIGINT       NOT NULL,
    laboratorio       VARCHAR(255) NOT NULL,
    numero_patrimonio VARCHAR(255) NOT NULL,
    PRIMARY KEY (recurso_id),
    CONSTRAINT uk_equipamentos_patrimonio UNIQUE (numero_patrimonio),
    CONSTRAINT fk_equipamentos_recurso FOREIGN KEY (recurso_id) REFERENCES recursos (id)
) ENGINE = InnoDB;

CREATE TABLE regras_cancelamento (
    id                         BIGINT  NOT NULL AUTO_INCREMENT,
    recurso_id                 BIGINT  NOT NULL,
    horas_minimas_antecedencia INT     NOT NULL,
    percentual_multa           DOUBLE  NOT NULL,
    permite_cancelamento       BOOLEAN NOT NULL,
    observacoes                VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT uk_regras_cancelamento_recurso UNIQUE (recurso_id),
    CONSTRAINT fk_regras_cancelamento_recurso FOREIGN KEY (recurso_id) REFERENCES recursos (id)
) ENGINE = InnoDB;

CREATE TABLE reservas (
    id                  BIGINT       NOT NULL AUTO_INCREMENT,
    usuario_id          BIGINT       NOT NULL,
    recurso_id          BIGINT       NOT NULL,
    data_hora_inicio    DATETIME(6)  NOT NULL,
    data_hora_fim       DATETIME(6)  NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    criado_em           DATETIME(6)  NOT NULL,
    cancelado_em        DATETIME(6),
    motivo_cancelamento VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT fk_reservas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id),
    CONSTRAINT fk_reservas_recurso FOREIGN KEY (recurso_id) REFERENCES recursos (id),
    CONSTRAINT ck_reservas_status CHECK (status IN ('PENDENTE', 'CONFIRMADA', 'CANCELADA')),
    CONSTRAINT ck_reservas_periodo CHECK (data_hora_fim > data_hora_inicio)
) ENGINE = InnoDB;

CREATE INDEX idx_reservas_recurso_periodo ON reservas (recurso_id, data_hora_inicio, data_hora_fim);
CREATE INDEX idx_reservas_usuario ON reservas (usuario_id);
