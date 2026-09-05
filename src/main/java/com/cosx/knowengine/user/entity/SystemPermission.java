package com.cosx.knowengine.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.cosx.knowengine.common.entity.BaseEntity;
import com.cosx.knowengine.user.enums.UserPermission;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_permission")
public class SystemPermission extends BaseEntity {

    @TableId
    private Long id;

    private UserPermission permission;

    private Integer status;

}
