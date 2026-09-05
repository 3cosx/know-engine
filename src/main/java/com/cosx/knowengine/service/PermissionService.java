package com.cosx.knowengine.service;

import com.cosx.knowengine.common.result.PageResponse;
import com.cosx.knowengine.dto.permission.PermissionCreateRequest;
import com.cosx.knowengine.dto.permission.PermissionQuery;
import com.cosx.knowengine.dto.permission.PermissionResponse;

public interface PermissionService {

    PermissionResponse create(PermissionCreateRequest request);

    PageResponse<PermissionResponse> page(PermissionQuery query);
}
