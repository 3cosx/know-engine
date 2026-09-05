package com.cosx.knowengine.dto.response;

import com.cosx.knowengine.entity.KnowledgeDocument;

import java.io.Serializable;
import java.time.LocalDateTime;

public record KnowledgeDocumentResponse(
        Long id,
        String title,
        String content,
        String tags,
        Integer status,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {

    public static KnowledgeDocumentResponse from(KnowledgeDocument document) {
        return new KnowledgeDocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getContent(),
                document.getTags(),
                document.getStatus(),
                document.getVersion(),
                document.getCreatedAt(),
                document.getUpdatedAt()
        );
    }
}
