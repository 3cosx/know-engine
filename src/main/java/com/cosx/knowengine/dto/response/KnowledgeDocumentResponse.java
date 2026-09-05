package com.cosx.knowengine.dto.response;

import com.cosx.knowengine.common.enums.DocumentStatus;
import com.cosx.knowengine.entity.KnowledgeDocument;

import java.io.Serializable;
import java.time.LocalDateTime;

public record KnowledgeDocumentResponse(
        Long id,
        Long documentId,
        String documentName,
        String content,
        DocumentStatus status,
        Long documentUser,
        String documentType,
        Integer segmentNumbers,
        Integer version,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {

    public static KnowledgeDocumentResponse from(KnowledgeDocument document) {
        return new KnowledgeDocumentResponse(
                document.getId(),
                document.getDocumentId(),
                document.getDocumentName(),
                document.getContent(),
                document.getStatus(),
                document.getDocumentUser(),
                document.getDocumentType(),
                document.getSegmentNumbers(),
                document.getVersion(),
                document.getCreateTime(),
                document.getUpdateTime()
        );
    }
}
