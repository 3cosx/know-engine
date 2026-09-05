package com.cosx.knowengine.document.dto;

import java.time.LocalDateTime;

public record DocumentSegmentResponse(
        String chunkId,
        String documentVersionId,
        Integer segmentIndex,
        String segmentContent,
        Integer tokenCount,
        String status,
        String statusName,
        Integer lockVersion,
        LocalDateTime updatedAt) {
}
