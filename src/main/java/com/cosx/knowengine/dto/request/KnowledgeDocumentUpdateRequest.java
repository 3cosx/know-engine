package com.cosx.knowengine.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record KnowledgeDocumentUpdateRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题不能超过 200 个字符")
        String title,

        @NotBlank(message = "内容不能为空")
        String content,

        @Size(max = 500, message = "标签不能超过 500 个字符")
        String tags,

        @NotNull(message = "状态不能为空")
        @Min(value = 0, message = "状态只能是 0 或 1")
        @Max(value = 1, message = "状态只能是 0 或 1")
        Integer status,

        @NotNull(message = "版本号不能为空")
        @Min(value = 1, message = "版本号必须大于 0")
        Integer version
) {
}
