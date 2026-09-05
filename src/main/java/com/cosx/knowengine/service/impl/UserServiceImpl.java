package com.cosx.knowengine.service.impl;

import cn.dev33.satoken.secure.BCrypt;
import cn.dev33.satoken.stp.StpUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cosx.knowengine.common.result.PageResponse;
import com.cosx.knowengine.dto.user.GrantPermissionsRequest;
import com.cosx.knowengine.dto.user.UserCreateRequest;
import com.cosx.knowengine.dto.user.UserQuery;
import com.cosx.knowengine.dto.user.UserResponse;
import com.cosx.knowengine.dto.user.UserUpdateRequest;
import com.cosx.knowengine.entity.SystemPermission;
import com.cosx.knowengine.entity.UserInfo;
import com.cosx.knowengine.entity.UserPermissionRelation;
import com.cosx.knowengine.exception.BusinessException;
import com.cosx.knowengine.mapper.SystemPermissionMapper;
import com.cosx.knowengine.mapper.UserInfoMapper;
import com.cosx.knowengine.mapper.UserPermissionMapper;
import com.cosx.knowengine.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserInfoMapper userMapper;
    private final SystemPermissionMapper permissionMapper;
    private final UserPermissionMapper userPermissionMapper;

    @Override
    @Transactional
    public UserResponse create(UserCreateRequest request) {
        if (userMapper.selectCount(Wrappers.<UserInfo>lambdaQuery()
                .eq(UserInfo::getUsername, request.username())) > 0) {
            throw BusinessException.conflict("用户名已存在");
        }

        UserInfo user = new UserInfo();
        user.setUsername(request.username());
        user.setPasswordHash(BCrypt.hashpw(request.password()));
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setStatus(1);
        user.setVersion(1);
        userMapper.insert(user);
        return UserResponse.from(user, List.of());
    }

    @Override
    public UserResponse getById(Long id) {
        return toResponse(requireUser(id));
    }

    @Override
    public PageResponse<UserResponse> page(UserQuery query) {
        LambdaQueryWrapper<UserInfo> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(condition -> condition
                    .like(UserInfo::getUsername, query.getKeyword())
                    .or()
                    .like(UserInfo::getNickname, query.getKeyword())
                    .or()
                    .like(UserInfo::getEmail, query.getKeyword()));
        }
        wrapper.eq(query.getStatus() != null, UserInfo::getStatus, query.getStatus())
                .orderByDesc(UserInfo::getCreateTime);
        Page<UserInfo> page = userMapper.selectPage(Page.of(query.getPage(), query.getSize()), wrapper);
        return PageResponse.from(page, this::toResponse);
    }

    @Override
    @Transactional
    public UserResponse update(Long id, UserUpdateRequest request) {
        requireUser(id);
        UserInfo user = new UserInfo();
        user.setId(id);
        user.setNickname(request.nickname());
        user.setEmail(request.email());
        user.setStatus(request.status());
        user.setVersion(request.version());
        if (userMapper.updateById(user) != 1) {
            throw BusinessException.conflict("用户信息已被修改，请刷新后重试");
        }
        if (request.status() == 0) {
            StpUtil.logout(id);
        }
        return toResponse(requireUser(id));
    }

    @Override
    @Transactional
    public UserResponse grantPermissions(Long id, GrantPermissionsRequest request) {
        UserInfo user = requireUser(id);
        List<SystemPermission> permissions = permissionMapper.selectBatchIds(request.permissionIds());
        if (permissions.size() != request.permissionIds().size()) {
            throw BusinessException.conflict("权限列表中包含不存在的权限");
        }

        userPermissionMapper.deleteByUserId(id);
        for (Long permissionId : request.permissionIds()) {
            UserPermissionRelation relation = new UserPermissionRelation();
            relation.setUserId(id);
            relation.setPermissionId(permissionId);
            userPermissionMapper.insert(relation);
        }
        StpUtil.logout(id);
        return toResponse(user);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        requireUser(id);
        userPermissionMapper.deleteByUserId(id);
        if (userMapper.deleteById(id) != 1) {
            throw BusinessException.notFound("用户不存在");
        }
        StpUtil.logout(id);
    }

    private UserInfo requireUser(Long id) {
        UserInfo user = userMapper.selectById(id);
        if (user == null) {
            throw BusinessException.notFound("用户不存在");
        }
        return user;
    }

    private UserResponse toResponse(UserInfo user) {
        return UserResponse.from(user, permissionMapper.selectCodesByUserId(user.getId()));
    }
}
