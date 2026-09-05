package com.cosx.knowengine.document.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record SegmentVectorizeRequest(@NotNull @Positive Integer lockVersion) {
}
