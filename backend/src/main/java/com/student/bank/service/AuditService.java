package com.student.bank.service;

public interface AuditService {
    void record(String operationType, String resourceType, String resourceId, String result, String detail);
}
