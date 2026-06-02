package com.student.bank.service;

public interface EventPublisherService {
    void publish(String topic, String eventType, String businessKey, Object payload);
}
