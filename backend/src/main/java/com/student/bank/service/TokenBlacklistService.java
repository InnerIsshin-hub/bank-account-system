package com.student.bank.service;

public interface TokenBlacklistService {
    void add(String token, long expireEpochSecond);
    boolean contains(String token);
}
