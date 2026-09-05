package com.cosx.knowengine.user.service;

import com.cosx.knowengine.common.result.PageResponse;
import com.cosx.knowengine.user.dto.PermissionCreateRequest;
import com.cosx.knowengine.user.dto.PermissionQuery;
import com.cosx.knowengine.user.dto.PermissionResponse;

public interface PermissionService {

    PermissionResponse create(PermissionCreateRequest request);

    PageResponse<PermissionResponse> page(PermissionQuery query);
}
