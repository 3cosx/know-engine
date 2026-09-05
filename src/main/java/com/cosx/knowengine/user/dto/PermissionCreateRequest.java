package com.cosx.knowengine.user.dto;

import com.cosx.knowengine.user.enums.UserPermission;
import jakarta.validation.constraints.NotNull;

public record PermissionCreateRequest(
        @NotNull(message = "权限类型不能为空") UserPermission permission
) {
}
