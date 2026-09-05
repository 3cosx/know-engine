package com.cosx.knowengine.document.vector;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cosx.knowengine.document.enums.DocumentStatus;
import com.cosx.knowengine.document.enums.SegmentStatus;
import com.cosx.knowengine.document.persistence.mapper.DocumentSegmentMapper;
import com.cosx.knowengine.document.persistence.mapper.DocumentVersionMapper;
import com.cosx.knowengine.document.entity.DocumentProcessingTask;
import com.cosx.knowengine.document.entity.DocumentSegment;
import com.cosx.knowengine.document.entity.DocumentVersion;
import com.cosx.knowengine.document.entity.KnowledgeDocument;
import com.cosx.knowengine.exception.BusinessException;
import com.cosx.knowengine.document.persistence.mapper.KnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentVectorizationService {

    private final KnowledgeDocumentMapper documentMapper;
    private final DocumentVersionMapper versionMapper;
    private final DocumentSegmentMapper segmentMapper;
    private final DocumentEmbeddingModel embeddingModel;
    private final DocumentVectorStore vectorStore;
    private final VectorFingerprint fingerprint;
    private final TransactionTemplate transactionTemplate;

    public void vectorizeVersion(DocumentProcessingTask task) {
        KnowledgeDocument document = requireDocument(task.getDocumentId());
        List<DocumentSegment> allSegments = segmentMapper.selectList(Wrappers.<DocumentSegment>lambdaQuery()
                .eq(DocumentSegment::getDocumentVersionId, task.getDocumentVersionId())
                .orderByAsc(DocumentSegment::getSegmentIndex));
        if (allSegments.isEmpty()) {
            throw new BusinessException(42220, "文档解析后没有可向量化分段");
        }

        reuseUnchangedSegments(task, document, allSegments);

        List<DocumentSegment> pending = new ArrayList<>();
        for (DocumentSegment segment : allSegments) {
            String expectedHash = fingerprint.calculate(segment.getSegmentContent());
            if (SegmentStatus.EMBEDDED.equals(segment.getStatus())
                    && expectedHash.equals(segment.getVectorHash())
                    && vectorStore.exists(segment.getDocumentVersionId(), segment.getChunkId(), expectedHash)) {
                continue;
            }
            segment.setVectorHash(expectedHash);
            segment.setStatus(SegmentStatus.EMBEDDING);
            segmentMapper.updateById(segment);
            pending.add(segment);
        }

        if (!pending.isEmpty()) {
            try {
                List<float[]> vectors = embeddingModel.embedAll(pending.stream()
                        .map(DocumentSegment::getSegmentContent)
                        .toList());
                for (int index = 0; index < pending.size(); index++) {
                    persistVector(document, pending.get(index), vectors.get(index));
                }
            } catch (RuntimeException exception) {
                for (DocumentSegment segment : pending) {
                    segmentMapper.update(null, Wrappers.<DocumentSegment>lambdaUpdate()
                            .eq(DocumentSegment::getChunkId, segment.getChunkId())
                            .eq(DocumentSegment::getStatus, SegmentStatus.EMBEDDING)
                            .set(DocumentSegment::getStatus, SegmentStatus.FAILED));
                }
                updateVersionStatus(task.getDocumentVersionId(), DocumentStatus.FAILED);
                throw exception;
            }
        }

        long stored = vectorStore.countByVersion(task.getDocumentVersionId());
        if (stored != allSegments.size()) {
            throw BusinessException.upstream(50222, "Elasticsearch 向量数量校验失败");
        }
        activate(task);
    }

    public void vectorizeSingle(DocumentProcessingTask task) {
        KnowledgeDocument document = requireDocument(task.getDocumentId());
        if (!document.getCurrentVersionId().equals(task.getDocumentVersionId())) {
            throw BusinessException.conflict("分段所属版本已不是当前版本");
        }
        DocumentSegment segment = segmentMapper.selectOne(Wrappers.<DocumentSegment>lambdaQuery()
                .eq(DocumentSegment::getChunkId, task.getChunkId())
                .eq(DocumentSegment::getDocumentVersionId, task.getDocumentVersionId()));
        if (segment == null) {
            throw BusinessException.notFound("文档分段不存在");
        }
        String expectedHash = fingerprint.calculate(segment.getSegmentContent());
        if (!expectedHash.equals(task.getInputHash())) {
            throw BusinessException.conflict("分段内容已变化，向量化任务作废");
        }
        if (vectorStore.exists(segment.getDocumentVersionId(), segment.getChunkId(), expectedHash)) {
            markEmbedded(segment, expectedHash);
        } else {
            float[] vector = embeddingModel.embedAll(List.of(segment.getSegmentContent())).getFirst();
            persistVector(document, segment, vector);
        }
        aggregateVersionStatus(task.getDocumentVersionId());
    }

    private void persistVector(KnowledgeDocument document, DocumentSegment segment, float[] vector) {
        String vectorHash = fingerprint.calculate(segment.getSegmentContent());
        vectorStore.upsert(new DocumentVector(
                segment.getDocumentId(), segment.getDocumentVersionId(), segment.getChunkId(),
                document.getDocumentUser(), segment.getSegmentIndex(), segment.getSegmentContent(),
                vectorHash, vector));
        markEmbedded(segment, vectorHash);
    }

    private void reuseUnchangedSegments(
            DocumentProcessingTask task,
            KnowledgeDocument document,
            List<DocumentSegment> newSegments) {
        if (task.getPreviousVersionId() == null) {
            return;
        }
        List<DocumentSegment> oldSegments = segmentMapper.selectList(Wrappers.<DocumentSegment>lambdaQuery()
                .eq(DocumentSegment::getDocumentVersionId, task.getPreviousVersionId()));
        Map<Integer, DocumentSegment> oldByIndex = new HashMap<>();
        oldSegments.forEach(segment -> oldByIndex.put(segment.getSegmentIndex(), segment));
        for (DocumentSegment segment : newSegments) {
            DocumentSegment previous = oldByIndex.get(segment.getSegmentIndex());
            if (previous == null
                    || !previous.getSegmentContent().equals(segment.getSegmentContent())
                    || !SegmentStatus.EMBEDDED.equals(previous.getStatus())) {
                continue;
            }
            String expectedHash = fingerprint.calculate(segment.getSegmentContent());
            vectorStore.findVector(previous.getDocumentVersionId(), previous.getChunkId(), expectedHash)
                    .ifPresent(vector -> persistVector(document, segment, vector));
        }
    }

    private void markEmbedded(DocumentSegment segment, String vectorHash) {
        segmentMapper.update(null, Wrappers.<DocumentSegment>lambdaUpdate()
                .eq(DocumentSegment::getChunkId, segment.getChunkId())
                .eq(DocumentSegment::getDocumentVersionId, segment.getDocumentVersionId())
                .set(DocumentSegment::getEmbeddingId,
                        segment.getEmbeddingId() == null ? IdWorker.getId() : segment.getEmbeddingId())
                .set(DocumentSegment::getVectorHash, vectorHash)
                .set(DocumentSegment::getStatus, SegmentStatus.EMBEDDED)
                .setSql("lock_version = lock_version + 1"));
    }

    private void aggregateVersionStatus(Long versionId) {
        long failed = segmentMapper.selectCount(Wrappers.<DocumentSegment>lambdaQuery()
                .eq(DocumentSegment::getDocumentVersionId, versionId)
                .eq(DocumentSegment::getStatus, SegmentStatus.FAILED));
        long processing = segmentMapper.selectCount(Wrappers.<DocumentSegment>lambdaQuery()
                .eq(DocumentSegment::getDocumentVersionId, versionId)
                .eq(DocumentSegment::getStatus, SegmentStatus.EMBEDDING));
        long saved = segmentMapper.selectCount(Wrappers.<DocumentSegment>lambdaQuery()
                .eq(DocumentSegment::getDocumentVersionId, versionId)
                .in(DocumentSegment::getStatus, SegmentStatus.INIT, SegmentStatus.SAVED));
        DocumentStatus status = failed > 0 ? DocumentStatus.FAILED
                : processing > 0 ? DocumentStatus.EMBEDDING
                : saved > 0 ? DocumentStatus.SAVED
                : DocumentStatus.VECTORED;
        updateVersionStatus(versionId, status);
    }

    private KnowledgeDocument requireDocument(Long documentId) {
        KnowledgeDocument document = documentMapper.selectOne(Wrappers.<KnowledgeDocument>lambdaQuery()
                .eq(KnowledgeDocument::getDocumentId, documentId));
        if (document == null) {
            throw BusinessException.notFound("文档不存在");
        }
        return document;
    }

    private void updateVersionStatus(Long versionId, DocumentStatus status) {
        versionMapper.update(null, Wrappers.<DocumentVersion>lambdaUpdate()
                .eq(DocumentVersion::getDocumentVersionId, versionId)
                .set(DocumentVersion::getDocumentStatus, status));
    }

    public void markOldSegmentsReplaced(Long previousVersionId) {
        if (previousVersionId == null) {
            return;
        }
        transactionTemplate.executeWithoutResult(status -> segmentMapper.update(null,
                Wrappers.<DocumentSegment>lambdaUpdate()
                        .eq(DocumentSegment::getDocumentVersionId, previousVersionId)
                        .set(DocumentSegment::getStatus, SegmentStatus.REPLACED)));
    }

    private void activate(DocumentProcessingTask task) {
        transactionTemplate.executeWithoutResult(status -> {
            KnowledgeDocument document = requireDocument(task.getDocumentId());
            if (task.getPreviousVersionId() != null) {
                int updated = documentMapper.update(null, Wrappers.<KnowledgeDocument>lambdaUpdate()
                        .eq(KnowledgeDocument::getDocumentId, task.getDocumentId())
                        .eq(KnowledgeDocument::getCurrentVersionId, task.getPreviousVersionId())
                        .set(KnowledgeDocument::getCurrentVersionId, task.getDocumentVersionId())
                        .setSql("version = version + 1"));
                if (updated != 1) {
                    throw BusinessException.conflict("当前文档版本已变化，不能切换候选版本");
                }
                versionMapper.update(null, Wrappers.<DocumentVersion>lambdaUpdate()
                        .eq(DocumentVersion::getDocumentVersionId, task.getPreviousVersionId())
                        .set(DocumentVersion::getDocumentStatus, DocumentStatus.REPLACED)
                        .set(DocumentVersion::getDocumentVersionStatus,
                                com.cosx.knowengine.document.enums.DocumentVersionStatus.REPLACED));
            }
            versionMapper.update(null, Wrappers.<DocumentVersion>lambdaUpdate()
                    .eq(DocumentVersion::getDocumentVersionId, task.getDocumentVersionId())
                    .set(DocumentVersion::getDocumentStatus, DocumentStatus.VECTORED)
                    .set(DocumentVersion::getDocumentVersionStatus,
                            com.cosx.knowengine.document.enums.DocumentVersionStatus.ACTIVE));
        });
    }
}
