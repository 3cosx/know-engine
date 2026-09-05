package com.cosx.knowengine.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cosx.knowengine.common.entity.BaseEntity;
import com.cosx.knowengine.document.enums.SegmentStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document_segment")
public class DocumentSegment extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentId;

    private String documentName;

    private Long chunkId;

    private Long embeddingId;

    private Integer skipEmbedding;

    private String segmentContent;

    private String metadata;

    private SegmentStatus status;

    private Long documentVersionId;

    private Integer segmentIndex;

    private Integer tokenCount;

    private String vectorHash;

    @Version
    private Integer lockVersion;
}
