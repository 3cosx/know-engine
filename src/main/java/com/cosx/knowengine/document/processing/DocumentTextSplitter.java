package com.cosx.knowengine.document.processing;

import com.cosx.knowengine.document.config.DocumentProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentTextSplitter {

    private final DocumentProperties properties;

    public List<String> split(String markdown) {
        String normalized = markdown == null ? "" : markdown.replace("\r\n", "\n").strip();
        if (normalized.isEmpty()) {
            return List.of();
        }
        int segmentSize = Math.max(200, properties.getSegmentSize());
        int overlap = Math.clamp(properties.getSegmentOverlap(), 0, segmentSize / 2);
        List<String> result = new ArrayList<>();
        int start = 0;
        while (start < normalized.length()) {
            int targetEnd = Math.min(start + segmentSize, normalized.length());
            int end = findBoundary(normalized, start, targetEnd);
            String segment = normalized.substring(start, end).strip();
            if (!segment.isEmpty()) {
                result.add(segment);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(start + 1, end - overlap);
        }
        return result;
    }

    private int findBoundary(String content, int start, int targetEnd) {
        if (targetEnd >= content.length()) {
            return content.length();
        }
        int paragraph = content.lastIndexOf("\n\n", targetEnd);
        if (paragraph > start + (targetEnd - start) / 2) {
            return paragraph;
        }
        int newline = content.lastIndexOf('\n', targetEnd);
        return newline > start ? newline : targetEnd;
    }
}
