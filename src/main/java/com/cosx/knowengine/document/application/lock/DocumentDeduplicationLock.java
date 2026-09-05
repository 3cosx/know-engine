package com.cosx.knowengine.document.application.lock;

public interface DocumentDeduplicationLock {

    String tryLock(Long userId, String sourceHash);

    void unlock(Long userId, String sourceHash, String token);
}
