package com.cosx.knowengine.document.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DocumentTaskStatus {

    PENDING("pending", "等待处理"),
    PROCESSING("processing", "处理中"),
    SUCCEEDED("succeeded", "处理成功"),
    FAILED("failed", "处理失败"),
    CLEANUP_PENDING("cleanup_pending", "等待清理");

    @EnumValue
    private final String code;

    private final String name;
}
