package com.cosx.knowengine.document.parser;

import com.cosx.knowengine.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Component
public class DocumentArchiveExtractor {

    private static final int MAX_ENTRIES = 5000;
    private static final long MAX_UNCOMPRESSED_BYTES = 500L * 1024 * 1024;

    public ParsedDocument extractMarkdown(byte[] archive) {
        String selectedName = null;
        byte[] selectedContent = null;
        long totalBytes = 0;
        int entries = 0;

        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(archive))) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (++entries > MAX_ENTRIES) {
                    throw new BusinessException(42211, "MinerU 压缩包文件数量超限");
                }
                validateEntryName(entry.getName());
                if (entry.isDirectory()) {
                    continue;
                }
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int length;
                while ((length = zip.read(buffer)) != -1) {
                    totalBytes += length;
                    if (totalBytes > MAX_UNCOMPRESSED_BYTES) {
                        throw new BusinessException(42212, "MinerU 压缩包解压大小超限");
                    }
                    output.write(buffer, 0, length);
                }
                if (entry.getName().toLowerCase().endsWith(".md")
                        && (selectedContent == null || output.size() > selectedContent.length)) {
                    selectedName = Path.of(entry.getName()).getFileName().toString();
                    selectedContent = output.toByteArray();
                }
            }
        } catch (IOException exception) {
            throw new BusinessException(42210, "无法读取 MinerU 解析结果");
        }

        if (selectedContent == null) {
            throw new BusinessException(42213, "MinerU 解析结果中没有 Markdown 文件");
        }
        return new ParsedDocument(selectedName, new String(selectedContent, StandardCharsets.UTF_8));
    }

    private void validateEntryName(String name) {
        Path normalized = Path.of(name.replace('\\', '/')).normalize();
        if (normalized.isAbsolute() || normalized.startsWith("..")) {
            throw new BusinessException(42214, "MinerU 压缩包包含非法路径");
        }
    }
}
