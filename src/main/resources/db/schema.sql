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
    document_user BIGINT NOT NULL COMMENT 'Document owner identifier',
    current_version_id BIGINT NOT NULL COMMENT 'Current document version identifier',
    version INT NOT NULL DEFAULT 1 COMMENT 'Optimistic lock version',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) NOT NULL DEFAULT 'system',
    update_by VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_knowledge_document_id (document_id),
    KEY idx_knowledge_document_user_time (document_user, create_time),
    KEY idx_knowledge_document_current_version (current_version_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_version (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    document_version_id BIGINT NOT NULL COMMENT 'Snowflake document version identifier',
    document_id BIGINT NOT NULL COMMENT 'Snowflake document identifier',
    version_no INT NOT NULL COMMENT 'Sequential version number',
    document_name VARCHAR(255) NOT NULL COMMENT 'Version document name',
    converted_document_name VARCHAR(255) DEFAULT NULL COMMENT 'Converted document name',
    document_path VARCHAR(1024) DEFAULT NULL COMMENT 'Document storage path',
    content LONGTEXT NOT NULL COMMENT 'Version content snapshot',
    document_status VARCHAR(32) NOT NULL DEFAULT 'init' COMMENT 'DocumentStatus code',
    document_type VARCHAR(64) DEFAULT NULL COMMENT 'Document type',
    segment_numbers INT NOT NULL DEFAULT 0 COMMENT 'Version segment count',
    content_hash CHAR(64) NOT NULL COMMENT 'SHA-256 content hash',
    change_summary VARCHAR(500) DEFAULT NULL COMMENT 'Version change summary',
    source_version_id BIGINT DEFAULT NULL COMMENT 'Rollback source version identifier',
    document_version_status VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'DocumentVersionStatus code',
    lock_version INT NOT NULL DEFAULT 1 COMMENT 'Optimistic lock version',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) NOT NULL DEFAULT 'system',
    update_by VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_document_version_id (document_version_id),
    UNIQUE KEY uk_document_version_no (document_id, version_no),
    KEY idx_document_version_status (document_id, document_version_status),
    KEY idx_document_version_source (source_version_id),
    CONSTRAINT fk_document_version_document FOREIGN KEY (document_id)
        REFERENCES knowledge_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS document_segment (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT 'Primary key',
    document_id BIGINT NOT NULL COMMENT 'Snowflake document identifier',
    document_name VARCHAR(255) NOT NULL COMMENT 'Document name',
    chunk_id BIGINT NOT NULL COMMENT 'Snowflake chunk identifier',
    embedding_id BIGINT DEFAULT NULL COMMENT 'Snowflake embedding identifier',
    skip_embedding TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether to skip embedding',
    segment_content LONGTEXT NOT NULL COMMENT 'Segment content',
    metadata JSON DEFAULT NULL COMMENT 'Segment metadata',
    status VARCHAR(32) NOT NULL DEFAULT 'init' COMMENT 'SegmentStatus code',
    document_version_id BIGINT NOT NULL COMMENT 'Snowflake document version identifier',
    segment_index INT NOT NULL COMMENT 'Segment order within the version',
    token_count INT DEFAULT NULL COMMENT 'Segment token count',
    lock_version INT NOT NULL DEFAULT 1 COMMENT 'Optimistic lock version',
    is_deleted TINYINT NOT NULL DEFAULT 0 COMMENT 'Logical delete flag',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    create_by VARCHAR(64) NOT NULL DEFAULT 'system',
    update_by VARCHAR(64) NOT NULL DEFAULT 'system',
    PRIMARY KEY (id),
    UNIQUE KEY uk_document_segment_chunk (chunk_id),
    UNIQUE KEY uk_document_segment_order (document_version_id, segment_index),
    KEY idx_document_segment_document_time (document_id, create_time),
    KEY idx_document_segment_version_status (document_version_id, status),
    CONSTRAINT fk_document_segment_document FOREIGN KEY (document_id)
        REFERENCES knowledge_document (document_id),
    CONSTRAINT fk_document_segment_version FOREIGN KEY (document_version_id)
        REFERENCES document_version (document_version_id)
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
