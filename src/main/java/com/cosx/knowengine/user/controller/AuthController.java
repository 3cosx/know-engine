package com.cosx.knowengine.user.controller;

import com.cosx.knowengine.common.result.ApiResponse;
import com.cosx.knowengine.user.dto.BootstrapAdminRequest;
import com.cosx.knowengine.user.dto.LoginRequest;
import com.cosx.knowengine.user.dto.LoginResponse;
import com.cosx.knowengine.user.dto.UserResponse;
import com.cosx.knowengine.user.service.AuthService;
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
@RequestMapping("/v1/auth")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/bootstrap")
    public ResponseEntity<ApiResponse<LoginResponse>> bootstrap(
            @Valid @RequestBody BootstrapAdminRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(authService.bootstrapAdmin(request)));
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(authService.login(request));
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        authService.logout();
        return ApiResponse.success();
    }

    @GetMapping("/me")
    public ApiResponse<UserResponse> currentUser() {
        return ApiResponse.success(authService.currentUser());
    }
}
