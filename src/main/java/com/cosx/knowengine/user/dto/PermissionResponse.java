package com.cosx.knowengine.user.dto;

import com.cosx.knowengine.user.enums.UserPermission;
import com.cosx.knowengine.user.entity.SystemPermission;

import java.io.Serializable;
import java.time.LocalDateTime;

public record PermissionResponse(
        Long id,
        UserPermission permission,
        Integer status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) implements Serializable {

    public static PermissionResponse from(SystemPermission permission) {
        return new PermissionResponse(
                permission.getId(), permission.getPermission(), permission.getStatus(), permission.getCreateTime(),
                permission.getUpdateTime()
        );
    }
}
