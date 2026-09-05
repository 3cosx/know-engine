CREATE DATABASE IF NOT EXISTS know_engine
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE know_engine;

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT NOT NULL COMMENT 'Primary key',
    title VARCHAR(200) NOT NULL COMMENT 'Document title',
    content LONGTEXT NOT NULL COMMENT 'Document content',
    tags VARCHAR(500) DEFAULT NULL COMMENT 'Comma-separated tags',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-disabled, 1-enabled',
    version INT NOT NULL DEFAULT 1 COMMENT 'Optimistic lock version',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    KEY idx_knowledge_document_status (status),
    KEY idx_knowledge_document_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
