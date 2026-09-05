package com.cosx.knowengine.user.dto;

import com.cosx.knowengine.user.dto.UserResponse;

import java.io.Serializable;

public record LoginResponse(
        String tokenName,
        String tokenValue,
        UserResponse user
) implements Serializable {
}
