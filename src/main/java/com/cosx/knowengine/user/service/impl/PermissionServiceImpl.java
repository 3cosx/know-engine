package com.cosx.knowengine.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cosx.knowengine.common.result.PageResponse;
import com.cosx.knowengine.user.dto.PermissionCreateRequest;
import com.cosx.knowengine.user.dto.PermissionQuery;
import com.cosx.knowengine.user.dto.PermissionResponse;
import com.cosx.knowengine.user.entity.SystemPermission;
import com.cosx.knowengine.exception.BusinessException;
import com.cosx.knowengine.user.mapper.SystemPermissionMapper;
import com.cosx.knowengine.user.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissionServiceImpl implements PermissionService {

    private final SystemPermissionMapper permissionMapper;

    @Override
    @Transactional
    public PermissionResponse create(PermissionCreateRequest request) {
        if (permissionMapper.selectCount(Wrappers.<SystemPermission>lambdaQuery()
                .eq(SystemPermission::getPermission, request.permission())) > 0) {
            throw BusinessException.conflict("权限已存在");
        }

        SystemPermission permission = new SystemPermission();
        permission.setPermission(request.permission());
        permission.setStatus(1);
        permissionMapper.insert(permission);
        return PermissionResponse.from(permission);
    }

    @Override
    public PageResponse<PermissionResponse> page(PermissionQuery query) {
        LambdaQueryWrapper<SystemPermission> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(query.getPermission() != null, SystemPermission::getPermission, query.getPermission())
                .eq(query.getStatus() != null, SystemPermission::getStatus, query.getStatus())
                .orderByDesc(SystemPermission::getCreateTime);
        Page<SystemPermission> page = permissionMapper.selectPage(
                Page.of(query.getPage(), query.getSize()), wrapper);
        return PageResponse.from(page, PermissionResponse::from);
    }
}
