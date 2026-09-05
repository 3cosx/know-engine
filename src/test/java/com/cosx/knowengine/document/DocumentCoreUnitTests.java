package com.cosx.knowengine.document;

import com.cosx.knowengine.document.enums.SegmentStatus;
import com.cosx.knowengine.document.config.DocumentProperties;
import com.cosx.knowengine.document.dto.VersionIncrement;
import com.cosx.knowengine.document.parser.DocumentArchiveExtractor;
import com.cosx.knowengine.document.processing.DocumentTextSplitter;
import com.cosx.knowengine.document.support.SemanticVersion;
import com.cosx.knowengine.document.vector.DocumentEmbeddingModel;
import com.cosx.knowengine.document.vector.DocumentVectorStore;
import com.cosx.knowengine.document.vector.DocumentVectorizationService;
import com.cosx.knowengine.document.vector.VectorFingerprint;
import com.cosx.knowengine.document.vector.config.ElasticsearchProperties;
import com.cosx.knowengine.document.persistence.mapper.DocumentSegmentMapper;
import com.cosx.knowengine.document.persistence.mapper.DocumentVersionMapper;
import com.cosx.knowengine.document.entity.DocumentProcessingTask;
import com.cosx.knowengine.document.entity.DocumentSegment;
import com.cosx.knowengine.document.entity.KnowledgeDocument;
import com.cosx.knowengine.exception.BusinessException;
import com.cosx.knowengine.document.persistence.mapper.KnowledgeDocumentMapper;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentCoreUnitTests {

    @Test
    void shouldIncrementSemanticVersions() {
        SemanticVersion version = SemanticVersion.parse("1.2.3");

        assertThat(version.increment(VersionIncrement.PATCH).toString()).isEqualTo("1.2.4");
        assertThat(version.increment(VersionIncrement.MINOR).toString()).isEqualTo("1.3.0");
        assertThat(version.increment(VersionIncrement.MAJOR).toString()).isEqualTo("2.0.0");
        assertThatThrownBy(() -> SemanticVersion.parse("1.2"))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void shouldSplitWithOverlapWithoutProducingEmptySegments() {
        DocumentProperties properties = new DocumentProperties();
        properties.setSegmentSize(200);
        properties.setSegmentOverlap(20);
        DocumentTextSplitter splitter = new DocumentTextSplitter(properties);
        String content = "第一段内容。".repeat(30) + "\n\n" + "第二段内容。".repeat(30);

        List<String> segments = splitter.split(content);

        assertThat(segments).hasSizeGreaterThan(1).allMatch(segment -> !segment.isBlank());
    }

    @Test
    void shouldExtractMarkdownAndRejectZipSlip() throws Exception {
        DocumentArchiveExtractor extractor = new DocumentArchiveExtractor();
        byte[] valid = zip("result/document.md", "# title\ncontent");
        byte[] invalid = zip("../outside.md", "bad");

        assertThat(extractor.extractMarkdown(valid).markdown()).contains("# title");
        assertThatThrownBy(() -> extractor.extractMarkdown(invalid))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("非法路径");
    }

    @Test
    void vectorFingerprintShouldIncludeModelAndIndexIdentity() {
        ElasticsearchProperties elasticsearch = new ElasticsearchProperties();
        elasticsearch.setIndexSchemaVersion("v1");
        DocumentEmbeddingModel model = fixedModel("qwen:v1:3", 3);
        String first = new VectorFingerprint(model, elasticsearch).calculate(" content\r\n");

        elasticsearch.setIndexSchemaVersion("v2");
        String second = new VectorFingerprint(model, elasticsearch).calculate("content\n");

        assertThat(first).isNotEqualTo(second);
    }

    @Test
    void shouldNotCallQwenAgainWhenVectorFingerprintAlreadyExists() {
        KnowledgeDocumentMapper documentMapper = mock(KnowledgeDocumentMapper.class);
        DocumentVersionMapper versionMapper = mock(DocumentVersionMapper.class);
        DocumentSegmentMapper segmentMapper = mock(DocumentSegmentMapper.class);
        DocumentEmbeddingModel embeddingModel = mock(DocumentEmbeddingModel.class);
        DocumentVectorStore vectorStore = mock(DocumentVectorStore.class);
        VectorFingerprint fingerprint = mock(VectorFingerprint.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        DocumentVectorizationService service = new DocumentVectorizationService(
                documentMapper, versionMapper, segmentMapper, embeddingModel,
                vectorStore, fingerprint, transactionTemplate);

        KnowledgeDocument document = new KnowledgeDocument();
        document.setDocumentId(10L);
        document.setDocumentUser(20L);
        DocumentSegment segment = new DocumentSegment();
        segment.setDocumentId(10L);
        segment.setDocumentVersionId(30L);
        segment.setChunkId(40L);
        segment.setSegmentIndex(0);
        segment.setSegmentContent("content");
        segment.setVectorHash("same-hash");
        segment.setStatus(SegmentStatus.EMBEDDED);
        DocumentProcessingTask task = new DocumentProcessingTask();
        task.setDocumentId(10L);
        task.setDocumentVersionId(30L);

        when(documentMapper.selectOne(any())).thenReturn(document);
        when(segmentMapper.selectList(any())).thenReturn(List.of(segment));
        when(fingerprint.calculate("content")).thenReturn("same-hash");
        when(vectorStore.exists(30L, 40L, "same-hash")).thenReturn(true);
        when(vectorStore.countByVersion(30L)).thenReturn(1L);

        service.vectorizeVersion(task);

        verify(embeddingModel, never()).embedAll(any());
    }

    private DocumentEmbeddingModel fixedModel(String key, int dimensions) {
        return new DocumentEmbeddingModel() {
            @Override
            public List<float[]> embedAll(List<String> contents) {
                throw new UnsupportedOperationException();
            }

            @Override
            public String modelKey() {
                return key;
            }

            @Override
            public int dimensions() {
                return dimensions;
            }
        };
    }

    private byte[] zip(String name, String content) throws Exception {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            zip.putNextEntry(new ZipEntry(name));
            zip.write(content.getBytes(StandardCharsets.UTF_8));
            zip.closeEntry();
        }
        return bytes.toByteArray();
    }
}
