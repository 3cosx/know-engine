package com.cosx.knowengine.document.dto;

public record SegmentVectorizeResponse(
        String documentId,
        String documentVersionId,
        String chunkId,
        String taskId,
        String status,
        Integer lockVersion) {
}
