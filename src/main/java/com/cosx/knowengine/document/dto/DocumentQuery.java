package com.cosx.knowengine.document.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class DocumentQuery {

    @Min(1)
    private long page = 1;

    @Min(1)
    @Max(100)
    private long size = 20;

    private String keyword;

    private String status;
}
