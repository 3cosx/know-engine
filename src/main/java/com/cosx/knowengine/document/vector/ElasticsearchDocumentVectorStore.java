package com.cosx.knowengine.document.vector;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.cosx.knowengine.document.vector.config.ElasticsearchProperties;
import com.cosx.knowengine.document.vector.config.QwenProperties;
import com.cosx.knowengine.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.http.util.EntityUtils;
import org.elasticsearch.client.Request;
import org.elasticsearch.client.Response;
import org.elasticsearch.client.ResponseException;
import org.elasticsearch.client.RestClient;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
@RequiredArgsConstructor
public class ElasticsearchDocumentVectorStore implements DocumentVectorStore {

    private final RestClient restClient;
    private final ElasticsearchProperties properties;
    private final QwenProperties qwenProperties;

    @Override
    public void upsert(DocumentVector documentVector) {
        ensureIndex();
        Map<String, Object> source = new LinkedHashMap<>();
        source.put("documentId", documentVector.documentId().toString());
        source.put("documentVersionId", documentVector.documentVersionId().toString());
        source.put("chunkId", documentVector.chunkId().toString());
        source.put("documentUser", documentVector.documentUser().toString());
        source.put("segmentIndex", documentVector.segmentIndex());
        source.put("text", documentVector.content());
        source.put("vectorHash", documentVector.vectorHash());
        source.put("embedding", documentVector.vector());
        performJson("PUT", "/" + index() + "/_doc/" + stableId(documentVector.documentVersionId(), documentVector.chunkId()), source);
    }

    @Override
    public boolean exists(Long documentVersionId, Long chunkId, String vectorHash) {
        return findVector(documentVersionId, chunkId, vectorHash).isPresent();
    }

    @Override
    public Optional<float[]> findVector(Long documentVersionId, Long chunkId, String vectorHash) {
        Request request = new Request("GET", "/" + index() + "/_doc/" + stableId(documentVersionId, chunkId));
        try {
            Response response = restClient.performRequest(request);
            JSONObject body = JSON.parseObject(EntityUtils.toString(response.getEntity()));
            JSONObject source = body.getJSONObject("_source");
            if (source == null || !vectorHash.equals(source.getString("vectorHash"))) {
                return Optional.empty();
            }
            JSONArray values = source.getJSONArray("embedding");
            if (values == null) {
                return Optional.empty();
            }
            float[] vector = new float[values.size()];
            for (int index = 0; index < values.size(); index++) {
                vector[index] = values.getFloatValue(index);
            }
            return Optional.of(vector);
        } catch (ResponseException exception) {
            if (exception.getResponse().getStatusLine().getStatusCode() == 404) {
                return Optional.empty();
            }
            throw storageFailure(exception);
        } catch (IOException exception) {
            throw storageFailure(exception);
        }
    }

    @Override
    public long countByVersion(Long documentVersionId) {
        Map<String, Object> body = Map.of(
                "query", Map.of("term", Map.of("documentVersionId", documentVersionId.toString())));
        JSONObject response = performJson("POST", "/" + index() + "/_count", body);
        return response.getLongValue("count");
    }

    @Override
    public void deleteByVersion(Long documentUser, Long documentId, Long documentVersionId) {
        List<Map<String, Object>> filters = List.of(
                Map.of("term", Map.of("documentUser", documentUser.toString())),
                Map.of("term", Map.of("documentId", documentId.toString())),
                Map.of("term", Map.of("documentVersionId", documentVersionId.toString())));
        performJson("POST", "/" + index() + "/_delete_by_query?conflicts=proceed",
                Map.of("query", Map.of("bool", Map.of("filter", filters))));
    }

    private synchronized void ensureIndex() {
        Request exists = new Request("HEAD", "/" + index());
        try {
            restClient.performRequest(exists);
            return;
        } catch (ResponseException exception) {
            if (exception.getResponse().getStatusLine().getStatusCode() != 404) {
                throw storageFailure(exception);
            }
        } catch (IOException exception) {
            throw storageFailure(exception);
        }

        Map<String, Object> fieldMappings = new LinkedHashMap<>();
        fieldMappings.put("documentId", Map.of("type", "keyword"));
        fieldMappings.put("documentVersionId", Map.of("type", "keyword"));
        fieldMappings.put("chunkId", Map.of("type", "keyword"));
        fieldMappings.put("documentUser", Map.of("type", "keyword"));
        fieldMappings.put("segmentIndex", Map.of("type", "integer"));
        fieldMappings.put("text", Map.of("type", "text"));
        fieldMappings.put("vectorHash", Map.of("type", "keyword"));
        fieldMappings.put("embedding", Map.of(
                "type", "dense_vector",
                "dims", qwenProperties.getDimensions(),
                "index", true,
                "similarity", "cosine"));
        performJson("PUT", "/" + index(), Map.of("mappings", Map.of("properties", fieldMappings)));
    }

    private JSONObject performJson(String method, String endpoint, Object body) {
        Request request = new Request(method, endpoint);
        request.setJsonEntity(JSON.toJSONString(body));
        try {
            Response response = restClient.performRequest(request);
            if (response.getEntity() == null) {
                return new JSONObject();
            }
            return JSON.parseObject(EntityUtils.toString(response.getEntity()));
        } catch (IOException exception) {
            throw storageFailure(exception);
        }
    }

    private BusinessException storageFailure(Exception exception) {
        return BusinessException.upstream(50230, "Elasticsearch 向量存储操作失败");
    }

    private String index() {
        return properties.getIndexName();
    }

    private String stableId(Long documentVersionId, Long chunkId) {
        return documentVersionId + "_" + chunkId;
    }
}
