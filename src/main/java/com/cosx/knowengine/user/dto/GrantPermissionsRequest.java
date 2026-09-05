package com.cosx.knowengine.user.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;

import java.util.Set;

public record GrantPermissionsRequest(
        @NotEmpty(message = "权限 ID 列表不能为空")
        Set<@Positive(message = "权限 ID 必须大于 0") Long> permissionIds
) {
}
