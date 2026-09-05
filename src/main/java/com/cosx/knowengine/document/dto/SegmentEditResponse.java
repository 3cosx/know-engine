package com.cosx.knowengine.document.dto;

public record SegmentEditResponse(
        String documentId,
        String previousDocumentVersionId,
        String previousVersion,
        String documentVersionId,
        String version,
        String sourceChunkId,
        String chunkId,
        String taskId,
        String status) {
}
