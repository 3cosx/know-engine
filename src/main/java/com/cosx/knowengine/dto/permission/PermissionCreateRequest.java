package com.cosx.knowengine.dto.permission;

import com.cosx.knowengine.common.enums.UserPermission;
import jakarta.validation.constraints.NotNull;

public record PermissionCreateRequest(
        @NotNull(message = "权限类型不能为空") UserPermission permission
) {
}
