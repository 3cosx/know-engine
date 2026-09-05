package com.cosx.knowengine.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentStatus {

    INIT("init", "初始化"),
    SAVED("saved", "已保存"),
    EMBEDDING("embedding", "向量化中"),
    VECTORED("vectored", "已向量化"),
    DELETED("deleted", "已删除");

    @EnumValue
    private final String code;

    private final String name;
}
