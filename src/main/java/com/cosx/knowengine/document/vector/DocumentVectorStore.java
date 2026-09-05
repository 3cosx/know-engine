package com.cosx.knowengine.document.vector;

import java.util.Optional;

public interface DocumentVectorStore {

    void upsert(DocumentVector documentVector);

    boolean exists(Long documentVersionId, Long chunkId, String vectorHash);

    Optional<float[]> findVector(Long documentVersionId, Long chunkId, String vectorHash);

    long countByVersion(Long documentVersionId);

    void deleteByVersion(Long documentUser, Long documentId, Long documentVersionId);
}
