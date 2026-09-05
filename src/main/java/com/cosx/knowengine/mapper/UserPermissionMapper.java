package com.cosx.knowengine.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.cosx.knowengine.entity.UserPermissionRelation;
import org.apache.ibatis.annotations.Param;

public interface UserPermissionMapper extends BaseMapper<UserPermissionRelation> {

    int deleteByUserId(@Param("userId") Long userId);
}
