package com.cosx.knowengine.document.vector;

public record DocumentVector(
        Long documentId,
        Long documentVersionId,
        Long chunkId,
        Long documentUser,
        int segmentIndex,
        String content,
        String vectorHash,
        float[] vector) {
}
