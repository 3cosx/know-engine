package com.cosx.knowengine.user.service;

import com.cosx.knowengine.user.dto.BootstrapAdminRequest;
import com.cosx.knowengine.user.dto.LoginRequest;
import com.cosx.knowengine.user.dto.LoginResponse;
import com.cosx.knowengine.user.dto.UserResponse;

public interface AuthService {

    LoginResponse bootstrapAdmin(BootstrapAdminRequest request);

    LoginResponse login(LoginRequest request);

    void logout();

    UserResponse currentUser();
}
