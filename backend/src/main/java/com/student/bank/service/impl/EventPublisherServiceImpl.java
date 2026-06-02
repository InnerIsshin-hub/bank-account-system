package com.student.bank.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.student.bank.config.RabbitMqConfig;
import com.student.bank.entity.BankMessageOutbox;
import com.student.bank.mapper.BankMessageOutboxMapper;
import com.student.bank.service.EventPublisherService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EventPublisherServiceImpl implements EventPublisherService {
    private final BankMessageOutboxMapper outboxMapper;
    private final ObjectMapper objectMapper;
    private final ObjectProvider<RabbitTemplate> rabbitTemplateProvider;

    @Value("${bank.rabbit.enabled:true}")
    private boolean rabbitEnabled;

    @Override
    public void publish(String topic, String eventType, String businessKey, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            BankMessageOutbox event = new BankMessageOutbox();
            event.setTopic(topic);
            event.setEventType(eventType);
            event.setBusinessKey(businessKey);
            event.setPayload(json);
            event.setStatus("PENDING");
            event.setRetryCount(0);
            outboxMapper.insert(event);
            if (tryPublishRabbit(topic, eventType, json)) {
                event.setStatus("SENT");
                outboxMapper.updateById(event);
            }
        } catch (Exception e) {
            log.warn("event outbox write failed topic={}, type={}, key={}: {}", topic, eventType, businessKey, e.getMessage());
        }
    }

    private boolean tryPublishRabbit(String topic, String eventType, String payload) {
        if (!rabbitEnabled || !RabbitMqConfig.NOTIFICATION_EXCHANGE.equals(topic)) {
            return false;
        }
        try {
            RabbitTemplate rabbitTemplate = rabbitTemplateProvider.getIfAvailable();
            if (rabbitTemplate == null) {
                return false;
            }
            rabbitTemplate.convertAndSend(topic, notificationRoutingKey(eventType), payload);
            return true;
        } catch (Exception e) {
            log.warn("rabbitmq publish fallback topic={}, type={}: {}", topic, eventType, e.getMessage());
            return false;
        }
    }

    private String notificationRoutingKey(String eventType) {
        String normalized = eventType == null ? "" : eventType.toLowerCase();
        if (normalized.contains("sms")) {
            return "sms";
        }
        if (normalized.contains("email")) {
            return "email";
        }
        return "push";
    }
}
