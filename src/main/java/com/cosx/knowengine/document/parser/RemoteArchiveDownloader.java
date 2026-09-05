package com.cosx.knowengine.document.parser;

import com.cosx.knowengine.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@Component
public class RemoteArchiveDownloader {

    private static final int MAX_ARCHIVE_BYTES = 200 * 1024 * 1024;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    public byte[] download(String resultUrl) {
        URI uri;
        try {
            uri = URI.create(resultUrl);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(50211, "MinerU 返回了非法结果地址");
        }
        if (!"http".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(uri.getScheme())) {
            throw new BusinessException(50211, "MinerU 结果地址协议不受支持");
        }

        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMinutes(2))
                .GET()
                .build();
        try {
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new BusinessException(50212, "下载 MinerU 解析结果失败");
            }
            if (response.body().length > MAX_ARCHIVE_BYTES) {
                throw new BusinessException(42215, "MinerU 结果压缩包大小超限");
            }
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new BusinessException(50213, "下载 MinerU 解析结果被中断");
        } catch (IOException exception) {
            throw new BusinessException(50212, "下载 MinerU 解析结果失败");
        }
    }
}
