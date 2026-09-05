package com.cosx.knowengine.document.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentTaskStage {

    MINERU("mineru", "文档解析"),
    SPLITTING("splitting", "文档切分"),
    EMBEDDING("embedding", "向量化"),
    SWITCHING("switching", "版本切换"),
    CLEANUP("cleanup", "旧版本清理");

    @EnumValue
    private final String code;

    private final String name;
}
