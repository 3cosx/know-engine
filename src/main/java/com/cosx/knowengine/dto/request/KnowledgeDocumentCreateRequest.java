package com.cosx.knowengine.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record KnowledgeDocumentCreateRequest(
        @NotNull(message = "文档编号不能为空")
        @Positive(message = "文档编号必须大于 0")
        Long documentId,

        @NotBlank(message = "文档名称不能为空")
        @Size(max = 255, message = "文档名称不能超过 255 个字符")
        String documentName,

        @NotBlank(message = "内容不能为空")
        String content,

        @NotNull(message = "文档用户不能为空")
        @Positive(message = "文档用户必须大于 0")
        Long documentUser,

        @Size(max = 64, message = "文档类型不能超过 64 个字符")
        String documentType
) {
}
