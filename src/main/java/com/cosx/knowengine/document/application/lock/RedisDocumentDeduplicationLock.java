package com.cosx.knowengine.document.application.lock;

import com.cosx.knowengine.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class RedisDocumentDeduplicationLock implements DocumentDeduplicationLock {

    private static final Duration LOCK_TTL = Duration.ofMinutes(2);
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    private final StringRedisTemplate redisTemplate;

    @Override
    public String tryLock(Long userId, String sourceHash) {
        String token = UUID.randomUUID().toString();
        Boolean acquired = redisTemplate.opsForValue()
                .setIfAbsent(key(userId, sourceHash), token, LOCK_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            throw BusinessException.conflict("相同文件正在上传，请稍后重试");
        }
        return token;
    }

    @Override
    public void unlock(Long userId, String sourceHash, String token) {
        redisTemplate.execute(UNLOCK_SCRIPT, List.of(key(userId, sourceHash)), token);
    }

    private String key(Long userId, String sourceHash) {
        return "know-engine:document:upload:" + userId + ":" + sourceHash;
    }
}
