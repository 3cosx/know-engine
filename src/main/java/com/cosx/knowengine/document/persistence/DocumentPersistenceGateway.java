package com.cosx.knowengine.document.persistence;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cosx.knowengine.document.enums.DocumentStatus;
import com.cosx.knowengine.document.enums.DocumentTaskStage;
import com.cosx.knowengine.document.enums.DocumentTaskStatus;
import com.cosx.knowengine.document.enums.DocumentTaskType;
import com.cosx.knowengine.document.enums.DocumentVersionStatus;
import com.cosx.knowengine.document.enums.SegmentStatus;
import com.cosx.knowengine.document.dto.VersionIncrement;
import com.cosx.knowengine.document.persistence.mapper.DocumentProcessingTaskMapper;
import com.cosx.knowengine.document.persistence.mapper.DocumentSegmentMapper;
import com.cosx.knowengine.document.persistence.mapper.DocumentVersionMapper;
import com.cosx.knowengine.document.support.SemanticVersion;
import com.cosx.knowengine.document.support.Sha256Utils;
import com.cosx.knowengine.document.vector.DocumentEmbeddingModel;
import com.cosx.knowengine.document.entity.DocumentProcessingTask;
import com.cosx.knowengine.document.entity.DocumentSegment;
import com.cosx.knowengine.document.entity.DocumentVersion;
import com.cosx.knowengine.document.entity.KnowledgeDocument;
import com.cosx.knowengine.exception.BusinessException;
import com.cosx.knowengine.document.persistence.mapper.KnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentPersistenceGateway {

    private final KnowledgeDocumentMapper documentMapper;
    private final DocumentVersionMapper versionMapper;
    private final DocumentSegmentMapper segmentMapper;
    private final DocumentProcessingTaskMapper taskMapper;
    private final DocumentEmbeddingModel embeddingModel;

    @Transactional
    public CreatedRecords createInitial(
            Long userId, Long documentId, Long versionId, String name,
            String objectKey, String contentType, String sourceHash) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setDocumentId(documentId);
        document.setDocumentUser(userId);
        document.setCurrentVersionId(versionId);
        document.setVersion(1);
        documentMapper.insert(document);

        DocumentVersion version = newVersion(documentId, versionId, "1.0.0", name, objectKey,
                contentType, sourceHash, null, DocumentVersionStatus.ACTIVE);
        versionMapper.insert(version);
        DocumentProcessingTask task = newPipelineTask(documentId, version, null, sourceHash);
        taskMapper.insert(task);
        return new CreatedRecords(document, version, null, task);
    }

    @Transactional
    public CreatedRecords createNextVersion(
            KnowledgeDocument snapshot, Long versionId, String baseVersion, VersionIncrement increment,
            String name, String objectKey, String contentType, String sourceHash) {
        DocumentVersion current = versionMapper.selectCurrentVersionForUpdate(snapshot.getDocumentId());
        requireUnchangedCurrent(snapshot, current, baseVersion);
        requireNoRunningVersionTask(snapshot.getDocumentId());
        long duplicate = versionMapper.selectCount(Wrappers.<DocumentVersion>lambdaQuery()
                .eq(DocumentVersion::getDocumentId, snapshot.getDocumentId())
                .eq(DocumentVersion::getSourceHash, sourceHash));
        if (duplicate > 0) {
            throw BusinessException.conflict("该文件内容已经存在于当前文档版本中");
        }

        String nextVersion = SemanticVersion.parse(current.getVersionNo()).increment(increment).toString();
        DocumentVersion version = newVersion(snapshot.getDocumentId(), versionId, nextVersion, name,
                objectKey, contentType, sourceHash, current.getDocumentVersionId(),
                DocumentVersionStatus.INACTIVE);
        versionMapper.insert(version);
        DocumentProcessingTask task = newPipelineTask(snapshot.getDocumentId(), version, current, sourceHash);
        taskMapper.insert(task);
        return new CreatedRecords(snapshot, version, current, task);
    }

    @Transactional
    public SegmentPatch createSegmentPatch(
            KnowledgeDocument document, Long sourceChunkId, Integer lockVersion,
            String baseVersion, String requestedContent) {
        DocumentVersion current = versionMapper.selectCurrentVersionForUpdate(document.getDocumentId());
        requireUnchangedCurrent(document, current, baseVersion);
        requireNoRunningVersionTask(document.getDocumentId());
        DocumentSegment source = segmentMapper.selectOne(Wrappers.<DocumentSegment>lambdaQuery()
                .eq(DocumentSegment::getChunkId, sourceChunkId)
                .eq(DocumentSegment::getDocumentVersionId, current.getDocumentVersionId()));
        if (source == null) {
            throw BusinessException.notFound("当前版本中不存在该分段");
        }
        if (!source.getLockVersion().equals(lockVersion)) {
            throw BusinessException.conflict("分段已经被修改，请刷新后重试");
        }
        String newContent = requestedContent.strip();
        if (source.getSegmentContent().equals(newContent)) {
            throw BusinessException.conflict("分段内容没有变化");
        }

        List<DocumentSegment> oldSegments = segmentMapper.selectList(Wrappers.<DocumentSegment>lambdaQuery()
                .eq(DocumentSegment::getDocumentVersionId, current.getDocumentVersionId())
                .orderByAsc(DocumentSegment::getSegmentIndex));
        List<String> contents = oldSegments.stream()
                .map(segment -> segment.getChunkId().equals(sourceChunkId)
                        ? newContent : segment.getSegmentContent())
                .toList();
        String fullContent = String.join("\n\n", contents);
        String contentHash = Sha256Utils.digest(fullContent);
        if (versionMapper.selectCount(Wrappers.<DocumentVersion>lambdaQuery()
                .eq(DocumentVersion::getDocumentId, document.getDocumentId())
                .eq(DocumentVersion::getContentHash, contentHash)) > 0) {
            throw BusinessException.conflict("修改后的内容与已有版本相同");
        }

        Long versionId = IdWorker.getId();
        DocumentVersion version = new DocumentVersion();
        version.setDocumentVersionId(versionId);
        version.setDocumentId(document.getDocumentId());
        version.setVersionNo(SemanticVersion.parse(current.getVersionNo())
                .increment(VersionIncrement.PATCH).toString());
        version.setDocumentName(current.getDocumentName());
        version.setConvertedDocumentName(current.getConvertedDocumentName());
        version.setDocumentPath(current.getDocumentPath());
        version.setContent(fullContent);
        version.setDocumentStatus(DocumentStatus.EMBEDDING);
        version.setDocumentType(current.getDocumentType());
        version.setSegmentNumbers(oldSegments.size());
        version.setContentHash(contentHash);
        version.setChangeSummary("修改分段 " + source.getSegmentIndex());
        version.setSourceVersionId(current.getDocumentVersionId());
        version.setDocumentVersionStatus(DocumentVersionStatus.INACTIVE);
        version.setLockVersion(1);
        versionMapper.insert(version);

        DocumentSegment changed = null;
        for (int index = 0; index < oldSegments.size(); index++) {
            DocumentSegment old = oldSegments.get(index);
            DocumentSegment created = copySegment(old, versionId, contents.get(index));
            segmentMapper.insert(created);
            if (old.getChunkId().equals(sourceChunkId)) {
                changed = created;
            }
        }
        if (changed == null) {
            throw new IllegalStateException("修改后的分段快照创建失败");
        }

        DocumentProcessingTask task = new DocumentProcessingTask();
        task.setId(IdWorker.getId());
        task.setDocumentId(document.getDocumentId());
        task.setDocumentVersionId(versionId);
        task.setPreviousVersionId(current.getDocumentVersionId());
        task.setPreviousVersion(current.getVersionNo());
        task.setProvider("qwen");
        task.setTaskType(DocumentTaskType.SEGMENT_VERSION_PATCH);
        task.setChunkId(changed.getChunkId());
        task.setInputHash(contentHash);
        task.setModelKey(embeddingModel.modelKey());
        task.setIdempotencyKey(Sha256Utils.digest("SEGMENT_VERSION_PATCH\n"
                + current.getDocumentVersionId() + "\n" + sourceChunkId + "\n" + contentHash));
        initializeTask(task, DocumentTaskStage.EMBEDDING);
        taskMapper.insert(task);
        return new SegmentPatch(current, version, source, changed, task);
    }

    @Transactional
    public SegmentVectorTask createOrReuseVectorTask(DocumentSegment snapshot, String vectorHash) {
        String key = Sha256Utils.digest("SEGMENT_VECTORIZE\n" + snapshot.getDocumentVersionId()
                + "\n" + snapshot.getChunkId() + "\n" + vectorHash);
        DocumentProcessingTask existing = taskMapper.selectOne(Wrappers.<DocumentProcessingTask>lambdaQuery()
                .eq(DocumentProcessingTask::getIdempotencyKey, key));
        if (existing != null) {
            if (DocumentTaskStatus.FAILED.equals(existing.getStatus())) {
                taskMapper.update(null, Wrappers.<DocumentProcessingTask>lambdaUpdate()
                        .eq(DocumentProcessingTask::getId, existing.getId())
                        .eq(DocumentProcessingTask::getStatus, DocumentTaskStatus.FAILED)
                        .set(DocumentProcessingTask::getStatus, DocumentTaskStatus.PENDING)
                        .set(DocumentProcessingTask::getNextPollAt, LocalDateTime.now())
                        .set(DocumentProcessingTask::getErrorMessage, null));
                existing = taskMapper.selectById(existing.getId());
            }
            return new SegmentVectorTask(existing, snapshot);
        }
        int updated = segmentMapper.update(null, Wrappers.<DocumentSegment>lambdaUpdate()
                .eq(DocumentSegment::getChunkId, snapshot.getChunkId())
                .eq(DocumentSegment::getDocumentVersionId, snapshot.getDocumentVersionId())
                .eq(DocumentSegment::getLockVersion, snapshot.getLockVersion())
                .in(DocumentSegment::getStatus, SegmentStatus.SAVED, SegmentStatus.FAILED)
                .set(DocumentSegment::getStatus, SegmentStatus.EMBEDDING)
                .set(DocumentSegment::getVectorHash, vectorHash)
                .setSql("lock_version = lock_version + 1"));
        if (updated != 1) {
            throw BusinessException.conflict("分段已经被修改或正在向量化");
        }
        DocumentProcessingTask task = new DocumentProcessingTask();
        task.setId(IdWorker.getId());
        task.setDocumentId(snapshot.getDocumentId());
        task.setDocumentVersionId(snapshot.getDocumentVersionId());
        task.setProvider("qwen");
        task.setTaskType(DocumentTaskType.SEGMENT_VECTORIZE);
        task.setChunkId(snapshot.getChunkId());
        task.setInputHash(vectorHash);
        task.setModelKey(embeddingModel.modelKey());
        task.setIdempotencyKey(key);
        initializeTask(task, DocumentTaskStage.EMBEDDING);
        taskMapper.insert(task);
        DocumentSegment updatedSegment = segmentMapper.selectOne(Wrappers.<DocumentSegment>lambdaQuery()
                .eq(DocumentSegment::getChunkId, snapshot.getChunkId()));
        return new SegmentVectorTask(task, updatedSegment);
    }

    public DocumentProcessingTask getTask(Long taskId) {
        return taskMapper.selectById(taskId);
    }

    public boolean claim(Long taskId, DocumentTaskStatus expected) {
        return taskMapper.update(null, Wrappers.<DocumentProcessingTask>lambdaUpdate()
                .eq(DocumentProcessingTask::getId, taskId)
                .eq(DocumentProcessingTask::getStatus, expected)
                .set(DocumentProcessingTask::getStatus, DocumentTaskStatus.PROCESSING)
                .setSql("attempts = attempts + 1")
                .set(DocumentProcessingTask::getNextPollAt, null)) == 1;
    }

    @Transactional
    public void markSubmitted(Long taskId, String providerTaskId, LocalDateTime nextPollAt) {
        DocumentProcessingTask task = getTask(taskId);
        taskMapper.update(null, Wrappers.<DocumentProcessingTask>lambdaUpdate()
                .eq(DocumentProcessingTask::getId, taskId)
                .set(DocumentProcessingTask::getProviderTaskId, providerTaskId)
                .set(DocumentProcessingTask::getStatus, DocumentTaskStatus.PENDING)
                .set(DocumentProcessingTask::getNextPollAt, nextPollAt));
        updateVersionStatus(task.getDocumentVersionId(), DocumentStatus.PARSING);
    }

    public void reschedule(Long taskId, LocalDateTime nextPollAt) {
        taskMapper.update(null, Wrappers.<DocumentProcessingTask>lambdaUpdate()
                .eq(DocumentProcessingTask::getId, taskId)
                .set(DocumentProcessingTask::getStatus, DocumentTaskStatus.PENDING)
                .set(DocumentProcessingTask::getNextPollAt, nextPollAt));
    }

    @Transactional
    public void saveParsed(Long taskId, String convertedFilename, String markdown, List<String> contents) {
        DocumentProcessingTask task = getTask(taskId);
        DocumentVersion version = versionMapper.selectOne(Wrappers.<DocumentVersion>lambdaQuery()
                .eq(DocumentVersion::getDocumentVersionId, task.getDocumentVersionId()));
        if (segmentMapper.selectCount(Wrappers.<DocumentSegment>lambdaQuery()
                .eq(DocumentSegment::getDocumentVersionId, task.getDocumentVersionId())) == 0) {
            for (int index = 0; index < contents.size(); index++) {
                DocumentSegment segment = new DocumentSegment();
                segment.setDocumentId(task.getDocumentId());
                segment.setDocumentName(version.getDocumentName());
                segment.setChunkId(IdWorker.getId());
                segment.setSkipEmbedding(0);
                segment.setSegmentContent(contents.get(index));
                segment.setStatus(SegmentStatus.SAVED);
                segment.setDocumentVersionId(task.getDocumentVersionId());
                segment.setSegmentIndex(index);
                segment.setTokenCount(estimateTokens(contents.get(index)));
                segment.setLockVersion(1);
                segmentMapper.insert(segment);
            }
        }
        version.setConvertedDocumentName(convertedFilename);
        version.setContent(markdown);
        version.setContentHash(Sha256Utils.digest(markdown.replace("\r\n", "\n").strip()));
        version.setSegmentNumbers(contents.size());
        version.setDocumentStatus(DocumentStatus.EMBEDDING);
        versionMapper.updateById(version);
        updateTask(taskId, DocumentTaskStatus.PROCESSING, DocumentTaskStage.EMBEDDING, null, null);
    }

    @Transactional
    public void fail(Long taskId, String message) {
        DocumentProcessingTask task = getTask(taskId);
        if (task == null) {
            return;
        }
        String sanitized = sanitize(message);
        updateTask(taskId, DocumentTaskStatus.FAILED, task.getStage(), sanitized, null);
        if (task.getChunkId() != null && DocumentTaskType.SEGMENT_VECTORIZE.equals(task.getTaskType())) {
            segmentMapper.update(null, Wrappers.<DocumentSegment>lambdaUpdate()
                    .eq(DocumentSegment::getChunkId, task.getChunkId())
                    .eq(DocumentSegment::getStatus, SegmentStatus.EMBEDDING)
                    .set(DocumentSegment::getStatus, SegmentStatus.FAILED));
        }
        updateVersionStatus(task.getDocumentVersionId(), DocumentStatus.FAILED);
    }

    public void updateTaskStage(Long taskId, DocumentTaskStage stage) {
        taskMapper.update(null, Wrappers.<DocumentProcessingTask>lambdaUpdate()
                .eq(DocumentProcessingTask::getId, taskId)
                .set(DocumentProcessingTask::getStage, stage));
    }

    public void succeed(Long taskId) {
        updateTask(taskId, DocumentTaskStatus.SUCCEEDED, null, null, null);
    }

    public void cleanupPending(Long taskId, String message) {
        updateTask(taskId, DocumentTaskStatus.CLEANUP_PENDING, DocumentTaskStage.CLEANUP,
                sanitize(message), LocalDateTime.now().plusMinutes(1));
    }

    private DocumentSegment copySegment(DocumentSegment old, Long versionId, String content) {
        DocumentSegment created = new DocumentSegment();
        created.setDocumentId(old.getDocumentId());
        created.setDocumentName(old.getDocumentName());
        created.setChunkId(IdWorker.getId());
        created.setSkipEmbedding(old.getSkipEmbedding());
        created.setSegmentContent(content);
        created.setMetadata(old.getMetadata());
        created.setStatus(SegmentStatus.SAVED);
        created.setDocumentVersionId(versionId);
        created.setSegmentIndex(old.getSegmentIndex());
        created.setTokenCount(old.getSegmentContent().equals(content)
                ? old.getTokenCount() : estimateTokens(content));
        created.setLockVersion(1);
        return created;
    }

    private DocumentVersion newVersion(
            Long documentId, Long versionId, String versionNo, String name, String objectKey,
            String contentType, String sourceHash, Long sourceVersionId,
            DocumentVersionStatus versionStatus) {
        DocumentVersion version = new DocumentVersion();
        version.setDocumentVersionId(versionId);
        version.setDocumentId(documentId);
        version.setVersionNo(versionNo);
        version.setDocumentName(name);
        version.setDocumentPath(objectKey);
        version.setDocumentType(contentType);
        version.setDocumentStatus(DocumentStatus.UPLOADED);
        version.setSegmentNumbers(0);
        version.setSourceHash(sourceHash);
        version.setSourceVersionId(sourceVersionId);
        version.setDocumentVersionStatus(versionStatus);
        version.setLockVersion(1);
        return version;
    }

    private DocumentProcessingTask newPipelineTask(
            Long documentId, DocumentVersion version, DocumentVersion previous, String sourceHash) {
        DocumentProcessingTask task = new DocumentProcessingTask();
        task.setId(IdWorker.getId());
        task.setDocumentId(documentId);
        task.setDocumentVersionId(version.getDocumentVersionId());
        task.setPreviousVersionId(previous == null ? null : previous.getDocumentVersionId());
        task.setPreviousVersion(previous == null ? null : previous.getVersionNo());
        task.setProvider("mineru");
        task.setTaskType(DocumentTaskType.DOCUMENT_PIPELINE);
        task.setInputHash(sourceHash);
        task.setIdempotencyKey(Sha256Utils.digest("DOCUMENT_PIPELINE\n" + documentId
                + "\n" + version.getDocumentVersionId() + "\n" + sourceHash));
        initializeTask(task, DocumentTaskStage.MINERU);
        return task;
    }

    private void initializeTask(DocumentProcessingTask task, DocumentTaskStage stage) {
        task.setStatus(DocumentTaskStatus.PENDING);
        task.setStage(stage);
        task.setAttempts(0);
        task.setNextPollAt(LocalDateTime.now());
        task.setLockVersion(1);
    }

    private void requireUnchangedCurrent(
            KnowledgeDocument snapshot, DocumentVersion current, String baseVersion) {
        if (current == null || !current.getDocumentVersionId().equals(snapshot.getCurrentVersionId())
                || !current.getVersionNo().equals(baseVersion)) {
            throw BusinessException.conflict("文档版本已经变化，请刷新后重试");
        }
    }

    private void requireNoRunningVersionTask(Long documentId) {
        long running = taskMapper.selectCount(Wrappers.<DocumentProcessingTask>lambdaQuery()
                .eq(DocumentProcessingTask::getDocumentId, documentId)
                .in(DocumentProcessingTask::getStatus,
                        DocumentTaskStatus.PENDING, DocumentTaskStatus.PROCESSING));
        if (running > 0) {
            throw BusinessException.conflict("当前文档已有版本正在处理");
        }
    }

    private void updateTask(
            Long taskId, DocumentTaskStatus status, DocumentTaskStage stage,
            String errorMessage, LocalDateTime nextPollAt) {
        var update = Wrappers.<DocumentProcessingTask>lambdaUpdate()
                .eq(DocumentProcessingTask::getId, taskId)
                .set(DocumentProcessingTask::getStatus, status)
                .set(DocumentProcessingTask::getErrorMessage, errorMessage)
                .set(DocumentProcessingTask::getNextPollAt, nextPollAt);
        if (stage != null) {
            update.set(DocumentProcessingTask::getStage, stage);
        }
        taskMapper.update(null, update);
    }

    private void updateVersionStatus(Long versionId, DocumentStatus status) {
        versionMapper.update(null, Wrappers.<DocumentVersion>lambdaUpdate()
                .eq(DocumentVersion::getDocumentVersionId, versionId)
                .set(DocumentVersion::getDocumentStatus, status));
    }

    private int estimateTokens(String content) {
        return Math.max(1, (int) Math.ceil(content.codePointCount(0, content.length()) / 2.5));
    }

    private String sanitize(String message) {
        String value = message == null ? "文档处理失败" : message.replaceAll("https?://\\S+", "[URL]");
        return value.length() > 1000 ? value.substring(0, 1000) : value;
    }

    public record CreatedRecords(
            KnowledgeDocument document, DocumentVersion version,
            DocumentVersion previousVersion, DocumentProcessingTask task) {
    }

    public record SegmentPatch(
            DocumentVersion previousVersion, DocumentVersion version,
            DocumentSegment sourceSegment, DocumentSegment segment,
            DocumentProcessingTask task) {
    }

    public record SegmentVectorTask(DocumentProcessingTask task, DocumentSegment segment) {
    }
}
