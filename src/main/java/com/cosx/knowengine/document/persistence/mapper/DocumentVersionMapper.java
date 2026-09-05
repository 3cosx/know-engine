package com.cosx.knowengine.document.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cosx.knowengine.document.persistence.model.DocumentSummaryRow;
import com.cosx.knowengine.document.entity.DocumentVersion;
import org.apache.ibatis.annotations.Param;

public interface DocumentVersionMapper extends BaseMapper<DocumentVersion> {

    DocumentVersion selectDuplicateByUserAndSourceHash(
            @Param("documentUser") Long documentUser,
            @Param("sourceHash") String sourceHash);

    DocumentVersion selectCurrentVersionForUpdate(@Param("documentId") Long documentId);

    IPage<DocumentSummaryRow> selectDocumentPage(
            Page<DocumentSummaryRow> page,
            @Param("documentUser") Long documentUser,
            @Param("keyword") String keyword,
            @Param("status") String status);
}
