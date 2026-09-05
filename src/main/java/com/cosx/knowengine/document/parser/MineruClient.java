package com.cosx.knowengine.document.parser;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONPath;
import com.cosx.knowengine.document.parser.config.MineruProperties;
import com.cosx.knowengine.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class MineruClient {

    private final MineruProperties properties;

    public String submit(String fileUrl) {
        requireToken();
        String response = client().post()
                .uri("/api/v4/extract/task")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("url", fileUrl, "is_ocr", true, "enable_formula", true))
                .retrieve()
                .body(String.class);
        String taskId = firstString(response, "$.data.task_id", "$.data.taskId", "$.task_id");
        if (!StringUtils.hasText(taskId)) {
            throw new BusinessException(50210, "MinerU 未返回任务编号");
        }
        return taskId;
    }

    public MineruTaskResult query(String taskId) {
        requireToken();
        String response = client().get()
                .uri("/api/v4/extract/task/{taskId}", taskId)
                .retrieve()
                .body(String.class);
        String state = firstString(response, "$.data.state", "$.data.status", "$.state");
        String normalized = state == null ? "" : state.toLowerCase();
        if (normalized.contains("done") || normalized.contains("success")) {
            return new MineruTaskResult(
                    MineruTaskState.SUCCEEDED,
                    firstString(response, "$.data.full_zip_url", "$.data.zip_url", "$.data.result_url"),
                    null);
        }
        if (normalized.contains("fail") || normalized.contains("error")) {
            return new MineruTaskResult(
                    MineruTaskState.FAILED,
                    null,
                    firstString(response, "$.data.err_msg", "$.data.error", "$.message"));
        }
        return new MineruTaskResult(MineruTaskState.PROCESSING, null, null);
    }

    private RestClient client() {
        return RestClient.builder()
                .baseUrl(properties.getBaseUrl())
                .defaultHeader("Authorization", "Bearer " + properties.getToken())
                .build();
    }

    private void requireToken() {
        if (!StringUtils.hasText(properties.getToken())) {
            throw new BusinessException(50020, "未配置 MINERU_TOKEN");
        }
    }

    private String firstString(String json, String... paths) {
        if (!StringUtils.hasText(json)) {
            return null;
        }
        Object root = JSON.parse(json);
        for (String path : paths) {
            Object value = JSONPath.eval(root, path);
            if (value != null && StringUtils.hasText(value.toString())) {
                return value.toString();
            }
        }
        return null;
    }
}
