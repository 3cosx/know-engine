package com.cosx.knowengine.document.dto;

import java.time.LocalDateTime;

public record DocumentDetailResponse(
        String documentId,
        String documentVersionId,
        String version,
        String documentName,
        String convertedDocumentName,
        String documentType,
        String documentStatus,
        String documentStatusName,
        Integer segmentNumbers,
        String contentHash,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
