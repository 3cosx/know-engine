package com.cosx.knowengine.document.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentVersionStatus {

    ACTIVE("active", "生效"),
    INACTIVE("inactive", "待生效"),
    REPLACED("replaced", "已被替换");


    @EnumValue
    private final String code;

    private final String name;
}
