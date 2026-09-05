package com.cosx.knowengine.dto.permission;

import com.cosx.knowengine.common.enums.UserPermission;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class PermissionQuery {

    @Min(value = 1, message = "页码必须大于 0")
    private long page = 1;

    @Min(value = 1, message = "每页数量必须大于 0")
    @Max(value = 100, message = "每页最多查询 100 条")
    private long size = 20;

    private UserPermission permission;

    @Min(value = 0, message = "状态只能是 0 或 1")
    @Max(value = 1, message = "状态只能是 0 或 1")
    private Integer status;

}
