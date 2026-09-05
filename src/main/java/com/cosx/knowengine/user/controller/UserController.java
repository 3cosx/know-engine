package com.cosx.knowengine.user.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.cosx.knowengine.common.result.ApiResponse;
import com.cosx.knowengine.common.result.PageResponse;
import com.cosx.knowengine.user.dto.GrantPermissionsRequest;
import com.cosx.knowengine.user.dto.UserCreateRequest;
import com.cosx.knowengine.user.dto.UserQuery;
import com.cosx.knowengine.user.dto.UserResponse;
import com.cosx.knowengine.user.dto.UserUpdateRequest;
import com.cosx.knowengine.user.service.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequiredArgsConstructor
@RequestMapping("/v1/users")
public class UserController {

    private final UserService userService;

    @PostMapping
    @SaCheckPermission("ADMIN")
    public ResponseEntity<ApiResponse<UserResponse>> create(@Valid @RequestBody UserCreateRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(userService.create(request)));
    }

    @GetMapping("/{id}")
    @SaCheckPermission("ADMIN")
    public ApiResponse<UserResponse> getById(@Positive @PathVariable Long id) {
        return ApiResponse.success(userService.getById(id));
    }

    @GetMapping
    @SaCheckPermission("ADMIN")
    public ApiResponse<PageResponse<UserResponse>> page(@Valid UserQuery query) {
        return ApiResponse.success(userService.page(query));
    }

    @PutMapping("/{id}")
    @SaCheckPermission("ADMIN")
    public ApiResponse<UserResponse> update(
            @Positive @PathVariable Long id,
            @Valid @RequestBody UserUpdateRequest request) {
        return ApiResponse.success(userService.update(id, request));
    }

    @PutMapping("/{id}/permissions")
    @SaCheckPermission("ADMIN")
    public ApiResponse<UserResponse> grantPermissions(
            @Positive @PathVariable Long id,
            @Valid @RequestBody GrantPermissionsRequest request) {
        return ApiResponse.success(userService.grantPermissions(id, request));
    }

    @DeleteMapping("/{id}")
    @SaCheckPermission("ADMIN")
    public ApiResponse<Void> delete(@Positive @PathVariable Long id) {
        userService.delete(id);
        return ApiResponse.success();
    }
}
