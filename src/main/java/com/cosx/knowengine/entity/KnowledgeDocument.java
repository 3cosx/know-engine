package com.cosx.knowengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cosx.knowengine.common.enums.DocumentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("knowledge_document")
public class KnowledgeDocument extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private String documentName;

    private String convertedDocumentName;

    private String documentPath;

    private String content;

    private DocumentStatus status;

    private Long documentUser;

    private String documentType;

    private Integer segmentNumbers;

    private Long currentVersionId;

    @Version
    private Integer version;
}
