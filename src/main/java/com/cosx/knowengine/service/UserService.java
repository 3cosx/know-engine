package com.cosx.knowengine.service;

import com.cosx.knowengine.common.result.PageResponse;
import com.cosx.knowengine.dto.user.GrantPermissionsRequest;
import com.cosx.knowengine.dto.user.UserCreateRequest;
import com.cosx.knowengine.dto.user.UserQuery;
import com.cosx.knowengine.dto.user.UserResponse;
import com.cosx.knowengine.dto.user.UserUpdateRequest;

public interface UserService {

    UserResponse create(UserCreateRequest request);

    UserResponse getById(Long id);

    PageResponse<UserResponse> page(UserQuery query);

    UserResponse update(Long id, UserUpdateRequest request);

    UserResponse grantPermissions(Long id, GrantPermissionsRequest request);

    void delete(Long id);
}
