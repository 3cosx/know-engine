package com.cosx.knowengine.document.processing;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cosx.knowengine.document.enums.DocumentTaskStage;
import com.cosx.knowengine.document.enums.DocumentTaskStatus;
import com.cosx.knowengine.document.enums.DocumentTaskType;
import com.cosx.knowengine.document.parser.DocumentArchiveExtractor;
import com.cosx.knowengine.document.parser.MineruClient;
import com.cosx.knowengine.document.parser.MineruTaskResult;
import com.cosx.knowengine.document.parser.MineruTaskState;
import com.cosx.knowengine.document.parser.ParsedDocument;
import com.cosx.knowengine.document.parser.RemoteArchiveDownloader;
import com.cosx.knowengine.document.parser.config.MineruProperties;
import com.cosx.knowengine.document.persistence.DocumentPersistenceGateway;
import com.cosx.knowengine.document.persistence.mapper.DocumentProcessingTaskMapper;
import com.cosx.knowengine.document.persistence.mapper.DocumentVersionMapper;
import com.cosx.knowengine.document.storage.ObjectStorage;
import com.cosx.knowengine.document.storage.config.MinioProperties;
import com.cosx.knowengine.document.vector.DocumentVectorStore;
import com.cosx.knowengine.document.vector.DocumentVectorizationService;
import com.cosx.knowengine.document.entity.DocumentProcessingTask;
import com.cosx.knowengine.document.entity.DocumentVersion;
import com.cosx.knowengine.document.entity.KnowledgeDocument;
import com.cosx.knowengine.document.persistence.mapper.KnowledgeDocumentMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DocumentProcessor {

    private final DocumentPersistenceGateway persistence;
    private final DocumentProcessingTaskMapper taskMapper;
    private final DocumentVersionMapper versionMapper;
    private final KnowledgeDocumentMapper documentMapper;
    private final ObjectStorage objectStorage;
    private final MinioProperties minioProperties;
    private final MineruProperties mineruProperties;
    private final MineruClient mineruClient;
    private final RemoteArchiveDownloader archiveDownloader;
    private final DocumentArchiveExtractor archiveExtractor;
    private final DocumentTextSplitter textSplitter;
    private final DocumentVectorizationService vectorizationService;
    private final DocumentVectorStore vectorStore;

    @Async
    public void dispatch(Long taskId) {
        DocumentProcessingTask snapshot = persistence.getTask(taskId);
        if (snapshot == null) {
            return;
        }
        DocumentTaskStatus expected = DocumentTaskStatus.CLEANUP_PENDING.equals(snapshot.getStatus())
                ? DocumentTaskStatus.CLEANUP_PENDING : DocumentTaskStatus.PENDING;
        if (!persistence.claim(taskId, expected)) {
            return;
        }
        DocumentProcessingTask task = persistence.getTask(taskId);
        try {
            if (DocumentTaskStatus.CLEANUP_PENDING.equals(expected)) {
                cleanupOldVersion(task);
            } else if (DocumentTaskType.DOCUMENT_PIPELINE.equals(task.getTaskType())) {
                processMineruPipeline(task);
            } else if (DocumentTaskType.SEGMENT_VERSION_PATCH.equals(task.getTaskType())) {
                processPreparedVersion(task);
            } else if (DocumentTaskType.SEGMENT_VECTORIZE.equals(task.getTaskType())) {
                vectorizationService.vectorizeSingle(task);
                persistence.succeed(taskId);
            }
        } catch (RuntimeException exception) {
            log.error("Document task {} failed at stage {}", taskId, task.getStage(), exception);
            persistence.fail(taskId, exception.getMessage());
        }
    }

    @Scheduled(fixedDelayString = "${know-engine.document.worker-delay:5000}")
    public void dispatchDueTasks() {
        List<DocumentProcessingTask> dueTasks = taskMapper.selectList(
                Wrappers.<DocumentProcessingTask>lambdaQuery()
                        .in(DocumentProcessingTask::getStatus,
                                DocumentTaskStatus.PENDING, DocumentTaskStatus.CLEANUP_PENDING)
                        .and(query -> query.isNull(DocumentProcessingTask::getNextPollAt)
                                .or().le(DocumentProcessingTask::getNextPollAt, LocalDateTime.now()))
                        .orderByAsc(DocumentProcessingTask::getNextPollAt)
                        .last("LIMIT 20"));
        dueTasks.forEach(task -> dispatch(task.getId()));
    }

    private void processMineruPipeline(DocumentProcessingTask task) {
        if (task.getProviderTaskId() == null) {
            DocumentVersion version = requireVersion(task.getDocumentVersionId());
            String fileUrl = objectStorage.presignedGetUrl(
                    version.getDocumentPath(), minioProperties.getPresignedExpiry());
            String providerTaskId = mineruClient.submit(fileUrl);
            persistence.markSubmitted(task.getId(), providerTaskId,
                    LocalDateTime.now().plus(mineruProperties.getPollInterval()));
            return;
        }

        MineruTaskResult result = mineruClient.query(task.getProviderTaskId());
        if (MineruTaskState.PROCESSING.equals(result.state())) {
            if (task.getCreateTime() != null
                    && task.getCreateTime().plus(mineruProperties.getTimeout()).isBefore(LocalDateTime.now())) {
                throw new IllegalStateException("MinerU 文档解析超时");
            }
            persistence.reschedule(task.getId(),
                    LocalDateTime.now().plus(mineruProperties.getPollInterval()));
            return;
        }
        if (MineruTaskState.FAILED.equals(result.state())) {
            throw new IllegalStateException(result.errorMessage() == null
                    ? "MinerU 文档解析失败" : result.errorMessage());
        }
        if (result.resultUrl() == null) {
            throw new IllegalStateException("MinerU 未返回解析结果地址");
        }

        ParsedDocument parsed = archiveExtractor.extractMarkdown(
                archiveDownloader.download(result.resultUrl()));
        byte[] markdown = parsed.markdown().getBytes(StandardCharsets.UTF_8);
        objectStorage.put(convertedObjectKey(task.getDocumentId(), task.getDocumentVersionId()),
                new ByteArrayInputStream(markdown), markdown.length, "text/markdown");
        persistence.updateTaskStage(task.getId(), DocumentTaskStage.SPLITTING);
        persistence.saveParsed(task.getId(), parsed.filename(), parsed.markdown(),
                textSplitter.split(parsed.markdown()));
        processPreparedVersion(persistence.getTask(task.getId()));
    }

    private void processPreparedVersion(DocumentProcessingTask task) {
        vectorizationService.vectorizeVersion(task);
        persistence.updateTaskStage(task.getId(), DocumentTaskStage.CLEANUP);
        cleanupOldVersion(task);
    }

    private void cleanupOldVersion(DocumentProcessingTask task) {
        if (task.getPreviousVersionId() != null) {
            KnowledgeDocument document = documentMapper.selectOne(Wrappers.<KnowledgeDocument>lambdaQuery()
                    .eq(KnowledgeDocument::getDocumentId, task.getDocumentId()));
            try {
                vectorStore.deleteByVersion(document.getDocumentUser(),
                        task.getDocumentId(), task.getPreviousVersionId());
                vectorizationService.markOldSegmentsReplaced(task.getPreviousVersionId());
            } catch (RuntimeException exception) {
                persistence.cleanupPending(task.getId(), exception.getMessage());
                return;
            }
        }
        persistence.succeed(task.getId());
    }

    private DocumentVersion requireVersion(Long versionId) {
        DocumentVersion version = versionMapper.selectOne(Wrappers.<DocumentVersion>lambdaQuery()
                .eq(DocumentVersion::getDocumentVersionId, versionId));
        if (version == null) {
            throw new IllegalStateException("待处理文档版本不存在");
        }
        return version;
    }

    private String convertedObjectKey(Long documentId, Long versionId) {
        return "documents/" + documentId + "/versions/" + versionId + "/converted/document.md";
    }
}
