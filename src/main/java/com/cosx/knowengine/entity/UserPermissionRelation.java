package com.cosx.knowengine.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user_permission")
public class UserPermissionRelation extends BaseEntity {

    @TableId
    private Long id;

    private Long userId;

    private Long permissionId;
}
