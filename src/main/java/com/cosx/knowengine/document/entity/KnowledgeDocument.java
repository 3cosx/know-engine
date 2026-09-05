package com.cosx.knowengine.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cosx.knowengine.common.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_document")
public class KnowledgeDocument extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private Long documentUser;

    private Long currentVersionId;

    @Version
    private Integer version;
}
