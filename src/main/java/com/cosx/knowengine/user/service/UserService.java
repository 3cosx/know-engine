package com.cosx.knowengine.user.service;

import com.cosx.knowengine.common.result.PageResponse;
import com.cosx.knowengine.user.dto.GrantPermissionsRequest;
import com.cosx.knowengine.user.dto.UserCreateRequest;
import com.cosx.knowengine.user.dto.UserQuery;
import com.cosx.knowengine.user.dto.UserResponse;
import com.cosx.knowengine.user.dto.UserUpdateRequest;

public interface UserService {

    UserResponse create(UserCreateRequest request);

    UserResponse getById(Long id);

    PageResponse<UserResponse> page(UserQuery query);

    UserResponse update(Long id, UserUpdateRequest request);

    UserResponse grantPermissions(Long id, GrantPermissionsRequest request);

    void delete(Long id);
}
