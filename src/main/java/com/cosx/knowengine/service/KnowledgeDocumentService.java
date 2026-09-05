package com.cosx.knowengine.service;

import com.cosx.knowengine.common.result.PageResponse;
import com.cosx.knowengine.dto.request.KnowledgeDocumentCreateRequest;
import com.cosx.knowengine.dto.request.KnowledgeDocumentQuery;
import com.cosx.knowengine.dto.request.KnowledgeDocumentUpdateRequest;
import com.cosx.knowengine.dto.response.KnowledgeDocumentResponse;

public interface KnowledgeDocumentService {

    KnowledgeDocumentResponse create(KnowledgeDocumentCreateRequest request);

    KnowledgeDocumentResponse getById(Long id);

    PageResponse<KnowledgeDocumentResponse> page(KnowledgeDocumentQuery query);

    KnowledgeDocumentResponse update(Long id, KnowledgeDocumentUpdateRequest request);

    void delete(Long id);
}
