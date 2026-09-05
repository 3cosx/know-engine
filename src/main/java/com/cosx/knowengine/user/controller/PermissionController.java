package com.cosx.knowengine.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cosx.knowengine.common.result.ApiResponse;
import com.cosx.knowengine.common.result.PageResponse;
import com.cosx.knowengine.user.dto.PermissionCreateRequest;
import com.cosx.knowengine.user.dto.PermissionQuery;
import com.cosx.knowengine.user.dto.PermissionResponse;
import com.cosx.knowengine.user.service.PermissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/permissions")
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    @SaCheckPermission("ADMIN")
    public ResponseEntity<ApiResponse<PermissionResponse>> create(
            @Valid @RequestBody PermissionCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(permissionService.create(request)));
    }

    @GetMapping
    @SaCheckPermission("ADMIN")
    public ApiResponse<PageResponse<PermissionResponse>> page(@Valid PermissionQuery query) {
        return ApiResponse.success(permissionService.page(query));
    }
}
