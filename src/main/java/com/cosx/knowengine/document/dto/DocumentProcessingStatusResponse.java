package com.cosx.knowengine.document.dto;

public record DocumentProcessingStatusResponse(
        String documentId,
        String documentVersionId,
        String taskId,
        String status,
        String stage,
        String message) {
}
