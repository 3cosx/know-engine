package com.cosx.knowengine.dto.request;

import com.cosx.knowengine.common.enums.DocumentStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class KnowledgeDocumentQuery {

    @Min(value = 1, message = "页码必须大于 0")
    private long page = 1;

    @Min(value = 1, message = "每页数量必须大于 0")
    @Max(value = 100, message = "每页最多查询 100 条")
    private long size = 20;

    private String keyword;

    private DocumentStatus status;

}
