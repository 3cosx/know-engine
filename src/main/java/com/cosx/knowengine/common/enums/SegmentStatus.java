package com.cosx.knowengine.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SegmentStatus {

    INIT("init", "初始化"),
    SAVED("saved", "已保存"),
    EMBEDDING("embedding", "向量化中"),
    EMBEDDED("embedded", "向量化完成");

    @EnumValue
    private final String code;

    private final String name;
}
