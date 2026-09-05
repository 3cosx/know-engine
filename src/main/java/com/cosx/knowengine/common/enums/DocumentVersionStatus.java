package com.cosx.knowengine.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentVersionStatus {

    ACTIVE("active", "生效"),
    INACTIVE("inactive", "失效");


    @EnumValue
    private final String code;

    private final String name;
}
