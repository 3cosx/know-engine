package com.cosx.knowengine.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import com.cosx.knowengine.common.enums.DocumentStatus;
import com.cosx.knowengine.common.enums.DocumentVersionStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("document_version")
public class DocumentVersion extends BaseEntity {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long documentVersionId;

    private Long documentId;

    private Integer versionNo;

    private String documentName;

    private String convertedDocumentName;

    private String documentPath;

    private String content;

    private DocumentStatus documentStatus;

    private Long documentUser;

    private String documentType;

    private Integer segmentNumbers;

    private String contentHash;

    private String changeSummary;

    private Long sourceVersionId;

    private DocumentVersionStatus documentVersionStatus;

    @Version
    private Integer lockVersion;
}
