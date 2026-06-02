package com.student.bank.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(prefix = "bank.rabbit", name = "enabled", havingValue = "true", matchIfMissing = true)
public class RabbitMqConfig {
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";
    public static final String SMS_QUEUE = "notification.sms.queue";
    public static final String EMAIL_QUEUE = "notification.email.queue";
    public static final String PUSH_QUEUE = "notification.push.queue";

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE, true, false);
    }

    @Bean
    public Queue notificationSmsQueue() {
        return new Queue(SMS_QUEUE, true);
    }

    @Bean
    public Queue notificationEmailQueue() {
        return new Queue(EMAIL_QUEUE, true);
    }

    @Bean
    public Queue notificationPushQueue() {
        return new Queue(PUSH_QUEUE, true);
    }

    @Bean
    public Binding smsBinding(DirectExchange notificationExchange, Queue notificationSmsQueue) {
        return BindingBuilder.bind(notificationSmsQueue).to(notificationExchange).with("sms");
    }

    @Bean
    public Binding emailBinding(DirectExchange notificationExchange, Queue notificationEmailQueue) {
        return BindingBuilder.bind(notificationEmailQueue).to(notificationExchange).with("email");
    }

    @Bean
    public Binding pushBinding(DirectExchange notificationExchange, Queue notificationPushQueue) {
        return BindingBuilder.bind(notificationPushQueue).to(notificationExchange).with("push");
    }
}
