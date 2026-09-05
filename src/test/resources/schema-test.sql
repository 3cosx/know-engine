DROP TABLE IF EXISTS knowledge_document;

CREATE TABLE knowledge_document (
    id BIGINT NOT NULL PRIMARY KEY,
    title VARCHAR(200) NOT NULL,
    content CLOB NOT NULL,
    tags VARCHAR(500),
    status TINYINT NOT NULL DEFAULT 1,
    version INT NOT NULL DEFAULT 1,
    deleted TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
