package com.cosx.knowengine.user.entity;

import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cosx.knowengine.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class UserInfo extends BaseEntity {

    @TableId
    private Long id;

    private String username;

    private String passwordHash;

    private String nickname;

    private String email;

    private Integer status;

    private LocalDateTime lastLoginAt;

    @Version
    private Integer version;

}
