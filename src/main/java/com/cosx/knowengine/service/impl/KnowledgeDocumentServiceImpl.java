package com.cosx.knowengine.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cosx.knowengine.common.result.PageResponse;
import com.cosx.knowengine.dto.request.KnowledgeDocumentCreateRequest;
import com.cosx.knowengine.dto.request.KnowledgeDocumentQuery;
import com.cosx.knowengine.dto.request.KnowledgeDocumentUpdateRequest;
import com.cosx.knowengine.dto.response.KnowledgeDocumentResponse;
import com.cosx.knowengine.entity.KnowledgeDocument;
import com.cosx.knowengine.exception.BusinessException;
import com.cosx.knowengine.mapper.KnowledgeDocumentMapper;
import com.cosx.knowengine.service.KnowledgeDocumentService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class KnowledgeDocumentServiceImpl implements KnowledgeDocumentService {

    public static final String CACHE_NAME = "knowledge-document";

    private final KnowledgeDocumentMapper mapper;

    public KnowledgeDocumentServiceImpl(KnowledgeDocumentMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
    public KnowledgeDocumentResponse create(KnowledgeDocumentCreateRequest request) {
        KnowledgeDocument document = new KnowledgeDocument();
        document.setTitle(request.title());
        document.setContent(request.content());
        document.setTags(request.tags());
        document.setStatus(1);
        document.setVersion(1);
        mapper.insert(document);
        return KnowledgeDocumentResponse.from(document);
    }

    @Override
    @Cacheable(cacheNames = CACHE_NAME, key = "#id", unless = "#result == null")
    public KnowledgeDocumentResponse getById(Long id) {
        KnowledgeDocument document = mapper.selectById(id);
        if (document == null) {
            throw BusinessException.notFound("知识条目不存在");
        }
        return KnowledgeDocumentResponse.from(document);
    }

    @Override
    public PageResponse<KnowledgeDocumentResponse> page(KnowledgeDocumentQuery query) {
        LambdaQueryWrapper<KnowledgeDocument> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(query.getKeyword())) {
            wrapper.and(condition -> condition
                    .like(KnowledgeDocument::getTitle, query.getKeyword())
                    .or()
                    .like(KnowledgeDocument::getContent, query.getKeyword())
                    .or()
                    .like(KnowledgeDocument::getTags, query.getKeyword()));
        }
        wrapper.eq(query.getStatus() != null, KnowledgeDocument::getStatus, query.getStatus())
                .orderByDesc(KnowledgeDocument::getCreatedAt);

        Page<KnowledgeDocument> page = mapper.selectPage(
                Page.of(query.getPage(), query.getSize()), wrapper);
        return PageResponse.from(page, KnowledgeDocumentResponse::from);
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
    public KnowledgeDocumentResponse update(Long id, KnowledgeDocumentUpdateRequest request) {
        if (mapper.selectById(id) == null) {
            throw BusinessException.notFound("知识条目不存在");
        }

        KnowledgeDocument document = new KnowledgeDocument();
        document.setId(id);
        document.setTitle(request.title());
        document.setContent(request.content());
        document.setTags(request.tags());
        document.setStatus(request.status());
        document.setVersion(request.version());
        if (mapper.updateById(document) != 1) {
            throw BusinessException.conflict("数据已被其他请求修改，请刷新后重试");
        }
        return KnowledgeDocumentResponse.from(mapper.selectById(id));
    }

    @Override
    @Transactional
    @CacheEvict(cacheNames = CACHE_NAME, allEntries = true)
    public void delete(Long id) {
        if (mapper.deleteById(id) != 1) {
            throw BusinessException.notFound("知识条目不存在");
        }
    }
}
