package com.cosx.knowengine.document;

import com.cosx.knowengine.document.dto.DocumentQuery;
import com.cosx.knowengine.document.persistence.DocumentPersistenceGateway;
import com.cosx.knowengine.document.persistence.mapper.DocumentVersionMapper;
import com.cosx.knowengine.document.service.DocumentService;
import com.cosx.knowengine.document.entity.DocumentVersion;
import com.cosx.knowengine.user.security.CurrentUser;
import com.cosx.knowengine.user.security.UserContextHolder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@ActiveProfiles("test")
@SpringBootTest
class DocumentPersistenceIntegrationTests {

    @Autowired
    private DocumentPersistenceGateway persistence;

    @Autowired
    private DocumentVersionMapper versionMapper;

    @Autowired
    private DocumentService documentService;

    @AfterEach
    void clearUser() {
        UserContextHolder.clear();
    }

    @Test
    void shouldPersistInitialSemanticVersionAndQueryCurrentDocument() {
        UserContextHolder.set(new CurrentUser(100L, "reader", "Reader", List.of("NORMAL")));

        DocumentPersistenceGateway.CreatedRecords records = persistence.createInitial(
                100L, 200L, 300L, "Guide", "documents/200/original/guide.pdf",
                "application/pdf", "a".repeat(64));
        DocumentVersion duplicate = versionMapper.selectDuplicateByUserAndSourceHash(100L, "a".repeat(64));
        DocumentQuery query = new DocumentQuery();

        assertThat(records.version().getVersionNo()).isEqualTo("1.0.0");
        assertThat(duplicate.getDocumentVersionId()).isEqualTo(300L);
        assertThat(documentService.page(query).records())
                .singleElement()
                .satisfies(item -> {
                    assertThat(item.documentId()).isEqualTo("200");
                    assertThat(item.currentDocumentVersionId()).isEqualTo("300");
                    assertThat(item.version()).isEqualTo("1.0.0");
                });
    }
}
