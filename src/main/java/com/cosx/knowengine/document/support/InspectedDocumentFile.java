package com.cosx.knowengine.document.support;

public record InspectedDocumentFile(
        String originalFilename,
        String contentType,
        long size,
        String sourceHash) {
}
