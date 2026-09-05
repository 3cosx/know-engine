package com.cosx.knowengine.document.dto;

public record DuplicateDocumentResponse(
        String documentId,
        String documentVersionId,
        String version) {
}
