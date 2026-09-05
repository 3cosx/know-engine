package com.cosx.knowengine.user.dto;

import com.cosx.knowengine.user.entity.UserInfo;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

public record UserResponse(
        Long id,
        String username,
        String nickname,
        String email,
        Integer status,
        Integer version,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        List<String> permissions
) implements Serializable {

    public static UserResponse from(UserInfo user, List<String> permissions) {
        return new UserResponse(
                user.getId(), user.getUsername(), user.getNickname(), user.getEmail(),
                user.getStatus(), user.getVersion(), user.getLastLoginAt(),
                user.getCreateTime(), user.getUpdateTime(), permissions
        );
    }
}
