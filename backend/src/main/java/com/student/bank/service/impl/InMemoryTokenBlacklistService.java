package com.student.bank.service.impl;

import com.student.bank.service.TokenBlacklistService;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryTokenBlacklistService implements TokenBlacklistService {
    private final Map<String, Long> blacklist = new ConcurrentHashMap<>();

    @Override
    public void add(String token, long expireEpochSecond) {
        cleanup();
        blacklist.put(token, expireEpochSecond);
    }

    @Override
    public boolean contains(String token) {
        cleanup();
        Long expireAt = blacklist.get(token);
        return expireAt != null && expireAt > Instant.now().getEpochSecond();
    }

    private void cleanup() {
        long now = Instant.now().getEpochSecond();
        blacklist.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
