package com.cosx.knowengine.document.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cosx.knowengine.common.entity.BaseEntity;
import com.cosx.knowengine.document.enums.DocumentTaskStage;
import com.cosx.knowengine.document.enums.DocumentTaskStatus;
import com.cosx.knowengine.document.enums.DocumentTaskType;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document_processing_task")
public class DocumentProcessingTask extends BaseEntity {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private Long documentId;

    private Long documentVersionId;

    private Long previousVersionId;

    private String previousVersion;

    private String provider;

    private String providerTaskId;

    private DocumentTaskType taskType;

    private Long chunkId;

    private String inputHash;

    private String modelKey;

    private String idempotencyKey;

    private DocumentTaskStatus status;

    private DocumentTaskStage stage;

    private Integer attempts;

    private LocalDateTime nextPollAt;

    private String errorMessage;

    @Version
    private Integer lockVersion;
}
