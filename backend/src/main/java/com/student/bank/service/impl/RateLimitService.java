package com.student.bank.service.impl;

import com.student.bank.common.BusinessException;
import com.student.bank.common.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {
    private final Map<String, Deque<Long>> windows = new ConcurrentHashMap<>();
    private final ObjectProvider<StringRedisTemplate> redisTemplateProvider;

    @Value("${bank.redis.enabled:true}")
    private boolean redisEnabled;

    public void check(String key, int maxRequests, Duration window) {
        if (redisEnabled && checkWithRedis(key, maxRequests, window)) {
            return;
        }
        long now = Instant.now().toEpochMilli();
        long oldestAllowed = now - window.toMillis();
        Deque<Long> deque = windows.computeIfAbsent(key, ignored -> new ArrayDeque<>());
        synchronized (deque) {
            while (!deque.isEmpty() && deque.peekFirst() < oldestAllowed) {
                deque.pollFirst();
            }
            if (deque.size() >= maxRequests) {
                throw new BusinessException(ErrorCode.RATE_LIMITED);
            }
            deque.addLast(now);
        }
    }

    private boolean checkWithRedis(String key, int maxRequests, Duration window) {
        try {
            StringRedisTemplate redisTemplate = redisTemplateProvider.getIfAvailable();
            if (redisTemplate == null) {
                return false;
            }
            String redisKey = "bank:rate-limit:" + key;
            Long count = redisTemplate.opsForValue().increment(redisKey);
            if (count != null && count == 1) {
                redisTemplate.expire(redisKey, window);
            }
            if (count != null && count > maxRequests) {
                throw new BusinessException(ErrorCode.RATE_LIMITED);
            }
            return true;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("redis rate limit fallback for key={}: {}", key, e.getMessage());
            return false;
        }
    }
}
