package com.cosx.knowengine.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cosx.knowengine.user.entity.SystemPermission;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SystemPermissionMapper extends BaseMapper<SystemPermission> {

    List<String> selectCodesByUserId(@Param("userId") Long userId);
}
