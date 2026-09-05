package com.cosx.knowengine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record KnowledgeDocumentCreateRequest(
        @NotBlank(message = "标题不能为空")
        @Size(max = 200, message = "标题不能超过 200 个字符")
        String title,

        @NotBlank(message = "内容不能为空")
        String content,

        @Size(max = 500, message = "标签不能超过 500 个字符")
        String tags
) {
}
