package com.cosx.knowengine.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SegmentEditRequest(
        @NotBlank String segmentContent,
        @NotNull @Positive Integer lockVersion,
        @NotBlank String baseVersion) {
}
