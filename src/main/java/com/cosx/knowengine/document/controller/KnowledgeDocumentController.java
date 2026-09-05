package com.cosx.knowengine.document.controller;

import com.cosx.knowengine.common.result.ApiResponse;
import com.cosx.knowengine.common.result.PageResponse;
import com.cosx.knowengine.document.dto.DocumentDetailResponse;
import com.cosx.knowengine.document.dto.DocumentListItemResponse;
import com.cosx.knowengine.document.dto.DocumentProcessingStatusResponse;
import com.cosx.knowengine.document.dto.DocumentQuery;
import com.cosx.knowengine.document.dto.DocumentSegmentResponse;
import com.cosx.knowengine.document.dto.DocumentUploadResponse;
import com.cosx.knowengine.document.dto.SegmentEditRequest;
import com.cosx.knowengine.document.dto.SegmentEditResponse;
import com.cosx.knowengine.document.dto.SegmentQuery;
import com.cosx.knowengine.document.dto.SegmentVectorizeRequest;
import com.cosx.knowengine.document.dto.SegmentVectorizeResponse;
import com.cosx.knowengine.document.dto.VersionIncrement;
import com.cosx.knowengine.document.service.DocumentService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/knowledge-documents")
public class KnowledgeDocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> upload(
            @RequestPart("file") MultipartFile file,
            @RequestParam(value = "documentName", required = false) String documentName) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(documentService.upload(file, documentName)));
    }

    @PostMapping(value = "/{documentId}/versions", consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<DocumentUploadResponse>> uploadVersion(
            @Positive @PathVariable Long documentId,
            @RequestPart("file") MultipartFile file,
            @NotBlank @RequestParam String baseVersion,
            @RequestParam(defaultValue = "PATCH") VersionIncrement increment,
            @RequestParam(value = "documentName", required = false) String documentName) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(documentService.uploadNewVersion(
                        documentId, file, baseVersion, increment, documentName)));
    }

    @GetMapping
    public ApiResponse<PageResponse<DocumentListItemResponse>> page(@Valid DocumentQuery query) {
        return ApiResponse.success(documentService.page(query));
    }

    @GetMapping("/{documentId}")
    public ApiResponse<DocumentDetailResponse> detail(@Positive @PathVariable Long documentId) {
        return ApiResponse.success(documentService.detail(documentId));
    }

    @GetMapping("/{documentId}/processing-status")
    public ApiResponse<DocumentProcessingStatusResponse> processingStatus(
            @Positive @PathVariable Long documentId) {
        return ApiResponse.success(documentService.processingStatus(documentId));
    }

    @GetMapping("/{documentId}/segments")
    public ApiResponse<PageResponse<DocumentSegmentResponse>> segments(
            @Positive @PathVariable Long documentId,
            @Valid SegmentQuery query) {
        return ApiResponse.success(documentService.segments(documentId, query));
    }

    @PutMapping("/{documentId}/segments/{chunkId}")
    public ResponseEntity<ApiResponse<SegmentEditResponse>> editSegment(
            @Positive @PathVariable Long documentId,
            @Positive @PathVariable Long chunkId,
            @Valid @RequestBody SegmentEditRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(documentService.editSegment(documentId, chunkId, request)));
    }

    @PostMapping("/{documentId}/segments/{chunkId}/vectorize")
    public ResponseEntity<ApiResponse<SegmentVectorizeResponse>> vectorizeSegment(
            @Positive @PathVariable Long documentId,
            @Positive @PathVariable Long chunkId,
            @Valid @RequestBody SegmentVectorizeRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(ApiResponse.success(documentService.vectorizeSegment(
                        documentId, chunkId, request.lockVersion())));
    }
}
