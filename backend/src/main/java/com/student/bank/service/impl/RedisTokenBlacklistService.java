package com.student.bank.service.impl;

import com.student.bank.service.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "bank.redis", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RedisTokenBlacklistService implements TokenBlacklistService {
    private static final String PREFIX = "bank:token:blacklist:";

    private final StringRedisTemplate redisTemplate;
    private final Map<String, Long> fallback = new ConcurrentHashMap<>();

    @Override
    public void add(String token, long expireEpochSecond) {
        long ttl = expireEpochSecond - Instant.now().getEpochSecond();
        if (ttl <= 0) {
            return;
        }
        fallback.put(token, expireEpochSecond);
        try {
            redisTemplate.opsForValue().set(PREFIX + token, "1", Duration.ofSeconds(ttl));
        } catch (Exception e) {
            log.warn("redis token blacklist write fallback: {}", e.getMessage());
        }
    }

    @Override
    public boolean contains(String token) {
        cleanupFallback();
        Long expireAt = fallback.get(token);
        if (expireAt != null && expireAt > Instant.now().getEpochSecond()) {
            return true;
        }
        try {
            return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX + token));
        } catch (Exception e) {
            log.warn("redis token blacklist read fallback: {}", e.getMessage());
            return false;
        }
    }

    private void cleanupFallback() {
        long now = Instant.now().getEpochSecond();
        fallback.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
