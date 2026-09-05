package com.cosx.knowengine.dto.auth;

import com.cosx.knowengine.dto.user.UserResponse;

import java.io.Serializable;

public record LoginResponse(
        String tokenName,
        String tokenValue,
        UserResponse user
) implements Serializable {
}
