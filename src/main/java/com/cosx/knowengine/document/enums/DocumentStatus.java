package com.cosx.knowengine.document.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentStatus {

    INIT("init", "初始化"),
    UPLOADED("uploaded", "已上传"),
    PARSING("parsing", "解析中"),
    SAVED("saved", "已保存"),
    SPLITTING("splitting", "切分中"),
    EMBEDDING("embedding", "向量化中"),
    VECTORED("vectored", "已向量化"),
    REPLACED("replaced", "已被替换"),
    FAILED("failed", "处理失败"),
    DELETED("deleted", "已删除");

    @EnumValue
    private final String code;

    private final String name;
}
