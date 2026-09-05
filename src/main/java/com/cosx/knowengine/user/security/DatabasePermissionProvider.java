package com.cosx.knowengine.user.security;

import cn.dev33.satoken.stp.StpInterface;
import com.cosx.knowengine.user.mapper.SystemPermissionMapper;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DatabasePermissionProvider implements StpInterface {

    private final SystemPermissionMapper permissionMapper;

    public DatabasePermissionProvider(SystemPermissionMapper permissionMapper) {
        this.permissionMapper = permissionMapper;
    }

    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        List<String> permissions = permissionMapper.selectCodesByUserId(Long.valueOf(loginId.toString()));
        if (permissions.contains("ADMIN") && !permissions.contains("NORMAL")) {
            return List.of("ADMIN", "NORMAL");
        }
        return permissions;
    }

    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return List.of();
    }
}
