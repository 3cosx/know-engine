package com.cosx.knowengine.document.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cosx.knowengine.document.enums.DocumentTaskStatus;
import com.cosx.knowengine.document.enums.SegmentStatus;
import com.cosx.knowengine.common.result.PageResponse;
import com.cosx.knowengine.document.application.lock.DocumentDeduplicationLock;
import com.cosx.knowengine.document.dto.DocumentDetailResponse;
import com.cosx.knowengine.document.dto.DocumentListItemResponse;
import com.cosx.knowengine.document.dto.DocumentProcessingStatusResponse;
import com.cosx.knowengine.document.dto.DocumentQuery;
import com.cosx.knowengine.document.dto.DocumentSegmentResponse;
import com.cosx.knowengine.document.dto.DocumentUploadResponse;
import com.cosx.knowengine.document.dto.DuplicateDocumentResponse;
import com.cosx.knowengine.document.dto.SegmentEditRequest;
import com.cosx.knowengine.document.dto.SegmentEditResponse;
import com.cosx.knowengine.document.dto.SegmentQuery;
import com.cosx.knowengine.document.dto.SegmentVectorizeResponse;
import com.cosx.knowengine.document.dto.VersionIncrement;
import com.cosx.knowengine.document.persistence.DocumentPersistenceGateway;
import com.cosx.knowengine.document.persistence.mapper.DocumentProcessingTaskMapper;
import com.cosx.knowengine.document.persistence.mapper.DocumentSegmentMapper;
import com.cosx.knowengine.document.persistence.mapper.DocumentVersionMapper;
import com.cosx.knowengine.document.persistence.model.DocumentSummaryRow;
import com.cosx.knowengine.document.processing.DocumentProcessor;
import com.cosx.knowengine.document.storage.ObjectStorage;
import com.cosx.knowengine.document.support.DocumentFileInspector;
import com.cosx.knowengine.document.support.InspectedDocumentFile;
import com.cosx.knowengine.document.vector.DocumentVectorStore;
import com.cosx.knowengine.document.vector.VectorFingerprint;
import com.cosx.knowengine.document.entity.DocumentProcessingTask;
import com.cosx.knowengine.document.entity.DocumentSegment;
import com.cosx.knowengine.document.entity.DocumentVersion;
import com.cosx.knowengine.document.entity.KnowledgeDocument;
import com.cosx.knowengine.exception.BusinessException;
import com.cosx.knowengine.document.persistence.mapper.KnowledgeDocumentMapper;
import com.cosx.knowengine.user.security.CurrentUser;
import com.cosx.knowengine.user.security.UserContextHolder;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentService {

    private final KnowledgeDocumentMapper documentMapper;
    private final DocumentVersionMapper versionMapper;
    private final DocumentSegmentMapper segmentMapper;
    private final DocumentProcessingTaskMapper taskMapper;
    private final DocumentPersistenceGateway persistence;
    private final DocumentFileInspector fileInspector;
    private final DocumentDeduplicationLock deduplicationLock;
    private final ObjectStorage objectStorage;
    private final VectorFingerprint fingerprint;
    private final DocumentVectorStore vectorStore;
    private final DocumentProcessor processor;

    public DocumentUploadResponse upload(MultipartFile file, String documentName) {
        CurrentUser user = UserContextHolder.requireCurrentUser();
        InspectedDocumentFile inspected = fileInspector.inspect(file);
        rejectDuplicate(user.id(), inspected.sourceHash());
        String token = deduplicationLock.tryLock(user.id(), inspected.sourceHash());
        try {
            rejectDuplicate(user.id(), inspected.sourceHash());
            Long documentId = IdWorker.getId();
            Long versionId = IdWorker.getId();
            String objectKey = originalObjectKey(documentId, versionId, inspected.originalFilename());
            uploadObject(file, inspected, objectKey);
            DocumentPersistenceGateway.CreatedRecords records;
            try {
                records = persistence.createInitial(user.id(), documentId, versionId,
                        resolvedName(documentName, inspected.originalFilename()), objectKey,
                        inspected.contentType(), inspected.sourceHash());
            } catch (RuntimeException exception) {
                cleanupObject(objectKey);
                throw exception;
            }
            processor.dispatch(records.task().getId());
            return uploadResponse(records);
        } finally {
            releaseLock(user.id(), inspected.sourceHash(), token);
        }
    }

    public DocumentUploadResponse uploadNewVersion(
            Long documentId, MultipartFile file, String baseVersion,
            VersionIncrement increment, String documentName) {
        KnowledgeDocument document = requireAccessible(documentId);
        InspectedDocumentFile inspected = fileInspector.inspect(file);
        if (versionMapper.selectCount(Wrappers.<DocumentVersion>lambdaQuery()
                .eq(DocumentVersion::getDocumentId, documentId)
                .eq(DocumentVersion::getSourceHash, inspected.sourceHash())) > 0) {
            throw BusinessException.conflict("该文件已经作为此文档的一个版本上传");
        }
        String token = deduplicationLock.tryLock(document.getDocumentUser(), inspected.sourceHash());
        Long versionId = IdWorker.getId();
        String objectKey = originalObjectKey(documentId, versionId, inspected.originalFilename());
        try {
            uploadObject(file, inspected, objectKey);
            DocumentPersistenceGateway.CreatedRecords records;
            try {
                records = persistence.createNextVersion(document, versionId, baseVersion,
                        increment == null ? VersionIncrement.PATCH : increment,
                        resolvedName(documentName, inspected.originalFilename()), objectKey,
                        inspected.contentType(), inspected.sourceHash());
            } catch (RuntimeException exception) {
                cleanupObject(objectKey);
                throw exception;
            }
            processor.dispatch(records.task().getId());
            return uploadResponse(records);
        } finally {
            releaseLock(document.getDocumentUser(), inspected.sourceHash(), token);
        }
    }

    public PageResponse<DocumentListItemResponse> page(DocumentQuery query) {
        IPage<DocumentSummaryRow> result = versionMapper.selectDocumentPage(
                Page.of(query.getPage(), query.getSize()), queryOwnerFilter(),
                query.getKeyword(), query.getStatus());
        return PageResponse.from(result, row -> new DocumentListItemResponse(
                row.getDocumentId().toString(), row.getCurrentDocumentVersionId().toString(),
                row.getVersionNo(), row.getDocumentName(), row.getDocumentType(),
                row.getDocumentStatus().getCode(), row.getDocumentStatus().getName(),
                row.getSegmentNumbers(), row.getUpdateTime()));
    }

    public DocumentDetailResponse detail(Long documentId) {
        KnowledgeDocument document = requireAccessible(documentId);
        DocumentVersion version = requireVersion(document.getCurrentVersionId());
        return new DocumentDetailResponse(documentId.toString(),
                version.getDocumentVersionId().toString(), version.getVersionNo(),
                version.getDocumentName(), version.getConvertedDocumentName(),
                version.getDocumentType(), version.getDocumentStatus().getCode(),
                version.getDocumentStatus().getName(), version.getSegmentNumbers(),
                version.getContentHash(), version.getCreateTime(), version.getUpdateTime());
    }

    public PageResponse<DocumentSegmentResponse> segments(Long documentId, SegmentQuery query) {
        KnowledgeDocument document = requireAccessible(documentId);
        Page<DocumentSegment> result = segmentMapper.selectPage(Page.of(query.getPage(), query.getSize()),
                Wrappers.<DocumentSegment>lambdaQuery()
                        .eq(DocumentSegment::getDocumentVersionId, document.getCurrentVersionId())
                        .eq(StringUtils.hasText(query.getStatus()), DocumentSegment::getStatus, query.getStatus())
                        .orderByAsc(DocumentSegment::getSegmentIndex));
        return PageResponse.from(result, segment -> new DocumentSegmentResponse(
                segment.getChunkId().toString(), segment.getDocumentVersionId().toString(),
                segment.getSegmentIndex(), segment.getSegmentContent(), segment.getTokenCount(),
                segment.getStatus().getCode(), segment.getStatus().getName(),
                segment.getLockVersion(), segment.getUpdateTime()));
    }

    public DocumentProcessingStatusResponse processingStatus(Long documentId) {
        requireAccessible(documentId);
        DocumentProcessingTask task = taskMapper.selectOne(Wrappers.<DocumentProcessingTask>lambdaQuery()
                .eq(DocumentProcessingTask::getDocumentId, documentId)
                .orderByDesc(DocumentProcessingTask::getCreateTime)
                .last("LIMIT 1"));
        if (task == null) {
            KnowledgeDocument document = requireAccessible(documentId);
            DocumentVersion version = requireVersion(document.getCurrentVersionId());
            return new DocumentProcessingStatusResponse(documentId.toString(),
                    version.getDocumentVersionId().toString(), null,
                    version.getDocumentStatus().getCode(), null, null);
        }
        return new DocumentProcessingStatusResponse(documentId.toString(),
                task.getDocumentVersionId().toString(), task.getId().toString(),
                task.getStatus().getCode(), task.getStage().getCode(), task.getErrorMessage());
    }

    public SegmentEditResponse editSegment(Long documentId, Long chunkId, SegmentEditRequest request) {
        KnowledgeDocument document = requireAccessible(documentId);
        DocumentPersistenceGateway.SegmentPatch records = persistence.createSegmentPatch(
                document, chunkId, request.lockVersion(), request.baseVersion(), request.segmentContent());
        processor.dispatch(records.task().getId());
        return new SegmentEditResponse(documentId.toString(),
                records.previousVersion().getDocumentVersionId().toString(),
                records.previousVersion().getVersionNo(), records.version().getDocumentVersionId().toString(),
                records.version().getVersionNo(), records.sourceSegment().getChunkId().toString(),
                records.segment().getChunkId().toString(), records.task().getId().toString(),
                records.version().getDocumentStatus().getCode());
    }

    public SegmentVectorizeResponse vectorizeSegment(Long documentId, Long chunkId, Integer lockVersion) {
        KnowledgeDocument document = requireAccessible(documentId);
        DocumentSegment segment = segmentMapper.selectOne(Wrappers.<DocumentSegment>lambdaQuery()
                .eq(DocumentSegment::getChunkId, chunkId)
                .eq(DocumentSegment::getDocumentVersionId, document.getCurrentVersionId()));
        if (segment == null) {
            throw BusinessException.notFound("当前文档版本中不存在该分段");
        }
        String vectorHash = fingerprint.calculate(segment.getSegmentContent());
        if (SegmentStatus.EMBEDDED.equals(segment.getStatus())
                && vectorHash.equals(segment.getVectorHash())
                && vectorStore.exists(segment.getDocumentVersionId(), segment.getChunkId(), vectorHash)) {
            return vectorResponse(documentId, segment, null);
        }
        if (SegmentStatus.EMBEDDING.equals(segment.getStatus())) {
            DocumentPersistenceGateway.SegmentVectorTask records =
                    persistence.createOrReuseVectorTask(segment, vectorHash);
            processor.dispatch(records.task().getId());
            return vectorResponse(documentId, records.segment(), records.task().getId());
        }
        if (!segment.getLockVersion().equals(lockVersion)) {
            throw BusinessException.conflict("分段已经被修改，请刷新后重试");
        }
        if (!SegmentStatus.SAVED.equals(segment.getStatus())
                && !SegmentStatus.FAILED.equals(segment.getStatus())) {
            throw BusinessException.conflict("当前分段状态不允许向量化");
        }
        DocumentPersistenceGateway.SegmentVectorTask records =
                persistence.createOrReuseVectorTask(segment, vectorHash);
        processor.dispatch(records.task().getId());
        return vectorResponse(documentId, records.segment(), records.task().getId());
    }

    private KnowledgeDocument requireAccessible(Long documentId) {
        KnowledgeDocument document = documentMapper.selectOne(Wrappers.<KnowledgeDocument>lambdaQuery()
                .eq(KnowledgeDocument::getDocumentId, documentId));
        if (document == null) {
            throw BusinessException.notFound("文档不存在");
        }
        CurrentUser user = UserContextHolder.requireCurrentUser();
        if (!document.getDocumentUser().equals(user.id()) && !user.permissions().contains("ADMIN")) {
            throw BusinessException.forbidden("无权访问该文档");
        }
        return document;
    }

    private Long queryOwnerFilter() {
        CurrentUser user = UserContextHolder.requireCurrentUser();
        return user.permissions().contains("ADMIN") ? null : user.id();
    }

    private DocumentVersion requireVersion(Long versionId) {
        DocumentVersion version = versionMapper.selectOne(Wrappers.<DocumentVersion>lambdaQuery()
                .eq(DocumentVersion::getDocumentVersionId, versionId));
        if (version == null) {
            throw BusinessException.notFound("文档版本不存在");
        }
        return version;
    }

    private void rejectDuplicate(Long userId, String sourceHash) {
        DocumentVersion duplicate = versionMapper.selectDuplicateByUserAndSourceHash(userId, sourceHash);
        if (duplicate != null) {
            throw BusinessException.conflict(40901, "相同文档已经上传",
                    new DuplicateDocumentResponse(duplicate.getDocumentId().toString(),
                            duplicate.getDocumentVersionId().toString(), duplicate.getVersionNo()));
        }
    }

    private void uploadObject(MultipartFile file, InspectedDocumentFile inspected, String objectKey) {
        try (InputStream input = file.getInputStream()) {
            objectStorage.put(objectKey, input, inspected.size(), inspected.contentType());
        } catch (IOException exception) {
            throw new BusinessException(50014, "无法读取上传文件");
        }
    }

    private DocumentUploadResponse uploadResponse(DocumentPersistenceGateway.CreatedRecords records) {
        DocumentVersion previous = records.previousVersion();
        return new DocumentUploadResponse(records.document().getDocumentId().toString(),
                previous == null ? null : previous.getDocumentVersionId().toString(),
                previous == null ? null : previous.getVersionNo(),
                records.version().getDocumentVersionId().toString(), records.version().getVersionNo(),
                records.task().getId().toString(), records.version().getDocumentStatus().getCode());
    }

    private SegmentVectorizeResponse vectorResponse(Long documentId, DocumentSegment segment, Long taskId) {
        return new SegmentVectorizeResponse(documentId.toString(),
                segment.getDocumentVersionId().toString(), segment.getChunkId().toString(),
                taskId == null ? null : taskId.toString(), segment.getStatus().getCode(),
                segment.getLockVersion());
    }

    private void cleanupObject(String objectKey) {
        try {
            objectStorage.delete(objectKey);
        } catch (RuntimeException exception) {
            log.warn("Failed to compensate uploaded object: {}", objectKey);
        }
    }

    private void releaseLock(Long userId, String hash, String token) {
        try {
            deduplicationLock.unlock(userId, hash, token);
        } catch (RuntimeException exception) {
            log.warn("Failed to release document upload lock for user {}", userId);
        }
    }

    private String resolvedName(String requested, String original) {
        return StringUtils.hasText(requested) ? requested.strip() : original;
    }

    private String originalObjectKey(Long documentId, Long versionId, String filename) {
        return "documents/" + documentId + "/versions/" + versionId + "/original/" + filename;
    }
}
