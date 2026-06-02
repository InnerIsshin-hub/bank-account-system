package com.student.bank.service;

import com.student.bank.entity.BankNotification;

import java.util.List;
import java.util.Map;

public interface NotificationService {
    void notify(Long userId, String title, String content, String businessType);
    List<BankNotification> listUnreadFirst(Long userId);
    long unreadCount(Long userId);
    void markRead(Long userId, Long id);
    Map<String, Object> center(Long userId);
}
