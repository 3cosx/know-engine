package com.cosx.knowengine.document.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentTaskType {

    DOCUMENT_PIPELINE("document_pipeline", "文档处理"),
    SEGMENT_VERSION_PATCH("segment_version_patch", "分段版本修改"),
    SEGMENT_VECTORIZE("segment_vectorize", "分段向量化");

    @EnumValue
    private final String code;

    private final String name;
}
