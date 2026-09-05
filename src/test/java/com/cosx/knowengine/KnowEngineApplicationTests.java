package com.cosx.knowengine;

import com.cosx.knowengine.dto.request.KnowledgeDocumentCreateRequest;
import com.cosx.knowengine.dto.request.KnowledgeDocumentQuery;
import com.cosx.knowengine.dto.request.KnowledgeDocumentUpdateRequest;
import com.cosx.knowengine.dto.response.KnowledgeDocumentResponse;
import com.cosx.knowengine.service.KnowledgeDocumentService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
@ActiveProfiles("test")
@SpringBootTest
class KnowEngineApplicationTests {

    @Autowired
    private KnowledgeDocumentService service;

    @Test
    void shouldCompleteDocumentLifecycle() {
        KnowledgeDocumentResponse created = service.create(
                new KnowledgeDocumentCreateRequest("Spring Boot 4", "JDK 21 project notes", "java,spring"));

        assertThat(created.id()).isNotNull();
        assertThat(service.getById(created.id()).title()).isEqualTo("Spring Boot 4");

        KnowledgeDocumentQuery query = new KnowledgeDocumentQuery();
        query.setKeyword("JDK 21");
        assertThat(service.page(query).total()).isEqualTo(1);

        KnowledgeDocumentResponse updated = service.update(
                created.id(),
                new KnowledgeDocumentUpdateRequest(
                        "Spring Boot 4 Notes", "Updated content", "java", 1, created.version()));
        assertThat(updated.version()).isEqualTo(created.version() + 1);

        service.delete(created.id());
        assertThat(service.page(new KnowledgeDocumentQuery()).total()).isZero();
    }
}
