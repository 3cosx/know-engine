package com.cosx.knowengine.document.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SegmentQuery {

    @Min(1)
    private long page = 1;

    @Min(1)
    @Max(100)
    private long size = 50;

    private String status;
}
