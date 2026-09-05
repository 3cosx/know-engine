package com.cosx.knowengine.controller;

import com.cosx.knowengine.common.result.ApiResponse;
import com.cosx.knowengine.common.result.PageResponse;
import com.cosx.knowengine.dto.request.KnowledgeDocumentCreateRequest;
import com.cosx.knowengine.dto.request.KnowledgeDocumentQuery;
import com.cosx.knowengine.dto.request.KnowledgeDocumentUpdateRequest;
import com.cosx.knowengine.dto.response.KnowledgeDocumentResponse;
import com.cosx.knowengine.service.KnowledgeDocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/v1/knowledge-documents")
public class KnowledgeDocumentController {

    private final KnowledgeDocumentService service;

    public KnowledgeDocumentController(KnowledgeDocumentService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<KnowledgeDocumentResponse>> create(
            @Valid @RequestBody KnowledgeDocumentCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(service.create(request)));
    }

    @GetMapping("/{id}")
    public ApiResponse<KnowledgeDocumentResponse> getById(
            @Positive(message = "ID 必须大于 0") @PathVariable Long id) {
        return ApiResponse.success(service.getById(id));
    }

    @GetMapping
    public ApiResponse<PageResponse<KnowledgeDocumentResponse>> page(
            @Valid KnowledgeDocumentQuery query) {
        return ApiResponse.success(service.page(query));
    }

    @PutMapping("/{id}")
    public ApiResponse<KnowledgeDocumentResponse> update(
            @Positive(message = "ID 必须大于 0") @PathVariable Long id,
            @Valid @RequestBody KnowledgeDocumentUpdateRequest request) {
        return ApiResponse.success(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @Positive(message = "ID 必须大于 0") @PathVariable Long id) {
        service.delete(id);
        return ApiResponse.success();
    }
}
