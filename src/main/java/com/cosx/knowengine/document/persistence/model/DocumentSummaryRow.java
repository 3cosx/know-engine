package com.cosx.knowengine.document.persistence.model;

import com.cosx.knowengine.document.enums.DocumentStatus;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class DocumentSummaryRow {

    private Long documentId;

    private Long currentDocumentVersionId;

    private String versionNo;

    private String documentName;

    private String documentType;

    private DocumentStatus documentStatus;

    private Integer segmentNumbers;

    private LocalDateTime updateTime;
}
