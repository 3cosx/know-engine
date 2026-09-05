package com.cosx.knowengine.document.storage;

import java.io.InputStream;
import java.time.Duration;

public interface ObjectStorage {

    void put(String objectKey, InputStream inputStream, long size, String contentType);

    InputStream get(String objectKey);

    String presignedGetUrl(String objectKey, Duration expiry);

    void delete(String objectKey);
}
