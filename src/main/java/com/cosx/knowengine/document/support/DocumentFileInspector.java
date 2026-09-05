package com.cosx.knowengine.document.support;

import com.cosx.knowengine.document.config.DocumentProperties;
import com.cosx.knowengine.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.apache.tika.Tika;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class DocumentFileInspector {

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "text/plain",
            "text/markdown",
            "text/html",
            "application/xhtml+xml"
    );

    private final DocumentProperties properties;
    private final Tika tika = new Tika();

    public InspectedDocumentFile inspect(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(40010, "上传文件不能为空");
        }
        if (file.getSize() > properties.getMaxFileSize()) {
            throw new BusinessException(40011, "上传文件超过大小限制");
        }

        String filename = sanitizeFilename(file.getOriginalFilename());
        try (InputStream detectionStream = file.getInputStream();
             InputStream hashStream = file.getInputStream()) {
            String contentType = tika.detect(detectionStream, filename);
            if (!ALLOWED_TYPES.contains(contentType)) {
                throw new BusinessException(40012, "不支持的文件类型: " + contentType);
            }
            return new InspectedDocumentFile(
                    filename,
                    contentType,
                    file.getSize(),
                    Sha256Utils.digest(hashStream));
        } catch (IOException exception) {
            throw new BusinessException(40013, "无法读取上传文件");
        }
    }

    private String sanitizeFilename(String originalFilename) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename : "document";
        filename = filename.replace('\\', '/');
        filename = filename.substring(filename.lastIndexOf('/') + 1);
        filename = filename.replaceAll("[\\p{Cntrl}]", "").trim();
        if (!StringUtils.hasText(filename) || ".".equals(filename) || "..".equals(filename)) {
            return "document";
        }
        return filename.length() > 200 ? filename.substring(filename.length() - 200) : filename;
    }
}
