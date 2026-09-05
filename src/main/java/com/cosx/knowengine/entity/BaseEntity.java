package com.cosx.knowengine.entity;

import java.time.LocalDateTime;

public class BaseEntity {

    private Integer isDeleted;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;

    private String creatBy;

    private String updateBy;
}
