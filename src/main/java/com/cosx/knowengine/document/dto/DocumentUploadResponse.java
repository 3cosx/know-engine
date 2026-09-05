package com.cosx.knowengine.document.dto;

public record DocumentUploadResponse(
        String documentId,
        String previousDocumentVersionId,
        String previousVersion,
        String documentVersionId,
        String version,
        String taskId,
        String status) {
}
