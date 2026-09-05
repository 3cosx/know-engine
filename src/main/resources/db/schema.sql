CREATE DATABASE IF NOT EXISTS know_engine
    DEFAULT CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE know_engine;

CREATE TABLE IF NOT EXISTS chat_conversation (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    conversation_id BIGINT NOT NULL COMMENT 'Snowflake conversation identifier',
    conversation_title VARCHAR(255) DEFAULT NULL COMMENT 'Conversation title',
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, ARCHIVED or DELETED',
    user_id BIGINT NOT NULL COMMENT 'User identifier',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) NOT NULL DEFAULT 'system',
    update_by VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_chat_conversation_id (conversation_id),
    KEY idx_chat_conversation_user_time (user_id, create_time),
    KEY idx_chat_conversation_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    conversation_id BIGINT NOT NULL COMMENT 'Snowflake conversation identifier',
    question LONGTEXT DEFAULT NULL COMMENT 'Original message content',
    converted_question LONGTEXT DEFAULT NULL COMMENT 'Converted message content',
    document_ids TEXT DEFAULT NULL COMMENT 'Associated document identifiers',
    section_ids TEXT DEFAULT NULL COMMENT 'Associated section identifiers',
    status VARCHAR(32) NOT NULL COMMENT 'USER, ASSISTANT or SYSTEM',
    user_id BIGINT NOT NULL COMMENT 'User identifier',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) NOT NULL DEFAULT 'system',
    update_by VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    KEY idx_chat_message_conversation_time (conversation_id, create_time),
    KEY idx_chat_message_user_time (user_id, create_time),
    KEY idx_chat_message_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS knowledge_document (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    document_id BIGINT NOT NULL COMMENT 'Snowflake document identifier',
    document_name VARCHAR(255) NOT NULL COMMENT 'Document name',
    converted_document_name VARCHAR(255) DEFAULT NULL COMMENT 'Converted document name',
    document_path VARCHAR(1024) DEFAULT NULL COMMENT 'Document storage path',
    content LONGTEXT NOT NULL COMMENT 'Document content',
    status VARCHAR(32) NOT NULL DEFAULT 'init' COMMENT 'DocumentStatus code',
    document_user BIGINT NOT NULL COMMENT 'Document owner identifier',
    document_type VARCHAR(64) DEFAULT NULL COMMENT 'Document type',
    segment_numbers INT NOT NULL DEFAULT 0 COMMENT 'Segment count',
    version INT NOT NULL DEFAULT 1 COMMENT 'Optimistic lock version',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) NOT NULL DEFAULT 'system',
    update_by VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_knowledge_document_id (document_id),
    KEY idx_knowledge_document_user_time (document_user, create_time),
    KEY idx_knowledge_document_status (status),
    KEY idx_knowledge_document_type (document_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_version (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    document_id BIGINT NOT NULL COMMENT 'Snowflake document identifier',
    version VARCHAR(64) NOT NULL COMMENT 'Document version',
    document_version_status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'DocumentVersionStatus code',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) NOT NULL DEFAULT 'system',
    update_by VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_document_version (document_id, version),
    KEY idx_document_version_status (document_version_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_segment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    document_id BIGINT NOT NULL COMMENT 'Snowflake document identifier',
    document_name VARCHAR(255) NOT NULL COMMENT 'Document name',
    document_version VARCHAR(64) DEFAULT NULL COMMENT 'Document version',
    chunk_id BIGINT DEFAULT NULL COMMENT 'Snowflake chunk identifier',
    embedding_id BIGINT DEFAULT NULL COMMENT 'Snowflake embedding identifier',
    skip_embedding TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether to skip embedding',
    segment_content LONGTEXT NOT NULL COMMENT 'Segment content',
    metadata JSON DEFAULT NULL COMMENT 'Segment metadata',
    status VARCHAR(32) NOT NULL DEFAULT 'init' COMMENT 'SegmentStatus code',
    document_version_id BIGINT DEFAULT NULL COMMENT 'Snowflake document version identifier',
    version INT NOT NULL DEFAULT 1 COMMENT 'Optimistic lock version',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) NOT NULL DEFAULT 'system',
    update_by VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    KEY idx_document_segment_document_time (document_id, create_time),
    KEY idx_document_segment_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL,
    username VARCHAR(32) NOT NULL,
    password_hash VARCHAR(100) NOT NULL,
    nickname VARCHAR(50) NOT NULL,
    email VARCHAR(100) DEFAULT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '0-disabled, 1-enabled',
    last_login_at DATETIME DEFAULT NULL,
    version INT NOT NULL DEFAULT 1,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) NOT NULL DEFAULT 'system',
    update_by VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_user_username (username),
    KEY idx_sys_user_status_create_time (status, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_permission (
    id BIGINT NOT NULL,
    permission VARCHAR(32) NOT NULL COMMENT 'NORMAL or ADMIN',
    status TINYINT NOT NULL DEFAULT 1,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) NOT NULL DEFAULT 'system',
    update_by VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_sys_permission (permission)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS sys_user_permission (
    id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    permission_id BIGINT NOT NULL,
    is_deleted TINYINT NOT NULL DEFAULT 0,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) NOT NULL DEFAULT 'system',
    update_by VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_permission (user_id, permission_id),
    KEY idx_user_permission_permission (permission_id),
    CONSTRAINT fk_user_permission_user FOREIGN KEY (user_id) REFERENCES sys_user (id),
    CONSTRAINT fk_user_permission_permission FOREIGN KEY (permission_id) REFERENCES sys_permission (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
