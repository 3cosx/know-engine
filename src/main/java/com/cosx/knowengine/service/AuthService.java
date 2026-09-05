package com.cosx.knowengine.service;

import com.cosx.knowengine.dto.auth.BootstrapAdminRequest;
import com.cosx.knowengine.dto.auth.LoginRequest;
import com.cosx.knowengine.dto.auth.LoginResponse;
import com.cosx.knowengine.dto.user.UserResponse;

public interface AuthService {

    LoginResponse bootstrapAdmin(BootstrapAdminRequest request);

    LoginResponse login(LoginRequest request);

    void logout();

    UserResponse currentUser();
}
