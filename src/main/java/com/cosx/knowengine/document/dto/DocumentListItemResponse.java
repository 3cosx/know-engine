package com.cosx.knowengine.document.dto;

import java.time.LocalDateTime;

public record DocumentListItemResponse(
        String documentId,
        String currentDocumentVersionId,
        String version,
        String documentName,
        String documentType,
        String documentStatus,
        String documentStatusName,
        Integer segmentNumbers,
        LocalDateTime updatedAt) {
}
