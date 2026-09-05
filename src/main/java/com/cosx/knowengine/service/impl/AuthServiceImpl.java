package com.cosx.knowengine.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.cosx.knowengine.common.enums.UserPermission;
import com.cosx.knowengine.dto.auth.BootstrapAdminRequest;
import com.cosx.knowengine.dto.auth.LoginRequest;
import com.cosx.knowengine.dto.auth.LoginResponse;
import com.cosx.knowengine.dto.user.UserResponse;
import com.cosx.knowengine.entity.SystemPermission;
import com.cosx.knowengine.entity.UserInfo;
import com.cosx.knowengine.entity.UserPermissionRelation;
import com.cosx.knowengine.exception.BusinessException;
import com.cosx.knowengine.mapper.SystemPermissionMapper;
import com.cosx.knowengine.mapper.UserInfoMapper;
import com.cosx.knowengine.mapper.UserPermissionMapper;
import com.cosx.knowengine.security.CurrentUser;
import com.cosx.knowengine.security.UserContextHolder;
import com.cosx.knowengine.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final UserInfoMapper userMapper;
    private final SystemPermissionMapper permissionMapper;
    private final UserPermissionMapper userPermissionMapper;

    @Override
    @Transactional
    public LoginResponse bootstrapAdmin(BootstrapAdminRequest request) {
        if (userMapper.selectCount(null) > 0) {
            throw BusinessException.conflict("系统已完成管理员初始化");
        }

        SystemPermission rootPermission = permissionMapper.selectOne(Wrappers.<SystemPermission>lambdaQuery()
                .eq(SystemPermission::getPermission, UserPermission.ADMIN));
        if (rootPermission == null) {
            rootPermission = new SystemPermission();
            rootPermission.setPermission(UserPermission.ADMIN);
            rootPermission.setStatus(1);
            permissionMapper.insert(rootPermission);
        }

        UserInfo admin = buildUser(request.username(), request.password(), request.nickname(), null);
        userMapper.insert(admin);

        UserPermissionRelation relation = new UserPermissionRelation();
        relation.setUserId(admin.getId());
        relation.setPermissionId(rootPermission.getId());
        userPermissionMapper.insert(relation);
        return loginUser(admin, List.of(UserPermission.ADMIN.name()));
    }

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        UserInfo user = userMapper.selectOne(Wrappers.<UserInfo>lambdaQuery()
                .eq(UserInfo::getUsername, request.username()));
        if (user == null || !BCrypt.checkpw(request.password(), user.getPasswordHash())) {
            throw BusinessException.unauthorized("用户名或密码错误");
        }
        if (user.getStatus() != 1) {
            throw BusinessException.forbidden("用户已被禁用");
        }

        user.setLastLoginAt(LocalDateTime.now());
        userMapper.updateById(user);
        return loginUser(user, permissionMapper.selectCodesByUserId(user.getId()));
    }

    @Override
    public void logout() {
        StpUtil.logout();
    }

    @Override
    public UserResponse currentUser() {
        CurrentUser current = UserContextHolder.requireCurrentUser();
        UserInfo user = userMapper.selectById(current.id());
        if (user == null || user.getStatus() != 1) {
            throw BusinessException.unauthorized("用户不存在或已被禁用");
        }
        return UserResponse.from(user, current.permissions());
    }

    private UserInfo buildUser(String username, String password, String nickname, String email) {
        UserInfo user = new UserInfo();
        user.setUsername(username);
        user.setPasswordHash(BCrypt.hashpw(password));
        user.setNickname(nickname);
        user.setEmail(email);
        user.setStatus(1);
        user.setVersion(1);
        return user;
    }

    private LoginResponse loginUser(UserInfo user, List<String> permissions) {
        StpUtil.login(user.getId());
        return new LoginResponse(
                StpUtil.getTokenName(),
                StpUtil.getTokenValue(),
                UserResponse.from(user, permissions)
        );
    }
}
