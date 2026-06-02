package com.student.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.student.bank.entity.BankNotification;
import com.student.bank.entity.BankNotificationSendRecord;
import com.student.bank.mapper.BankNotificationMapper;
import com.student.bank.mapper.BankNotificationSendRecordMapper;
import com.student.bank.service.EventPublisherService;
import com.student.bank.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {
    private final BankNotificationMapper notificationMapper;
    private final BankNotificationSendRecordMapper sendRecordMapper;
    private final EventPublisherService eventPublisherService;

    @Override
    public void notify(Long userId, String title, String content, String businessType) {
        BankNotification notification = new BankNotification();
        notification.setUserId(userId);
        notification.setTitle(title);
        notification.setContent(content);
        notification.setChannel("IN_APP");
        notification.setBusinessType(businessType);
        notification.setReadFlag(0);
        notificationMapper.insert(notification);

        BankNotificationSendRecord send = new BankNotificationSendRecord();
        send.setNotificationId(notification.getId());
        send.setUserId(userId);
        send.setChannel("IN_APP");
        send.setTemplateCode(businessType);
        send.setPayload(title + " - " + content);
        send.setSendStatus("SUCCESS");
        send.setRetryCount(0);
        sendRecordMapper.insert(send);

        eventPublisherService.publish("notification.exchange", businessType, String.valueOf(notification.getId()), notification);
    }

    @Override
    public List<BankNotification> listUnreadFirst(Long userId) {
        return notificationMapper.selectList(new LambdaQueryWrapper<BankNotification>()
                .eq(BankNotification::getUserId, userId)
                .orderByAsc(BankNotification::getReadFlag)
                .orderByDesc(BankNotification::getCreatedAt));
    }

    @Override
    public long unreadCount(Long userId) {
        return notificationMapper.selectCount(new LambdaQueryWrapper<BankNotification>()
                .eq(BankNotification::getUserId, userId)
                .eq(BankNotification::getReadFlag, 0));
    }

    @Override
    public void markRead(Long userId, Long id) {
        notificationMapper.update(null, new LambdaUpdateWrapper<BankNotification>()
                .eq(BankNotification::getId, id)
                .eq(BankNotification::getUserId, userId)
                .set(BankNotification::getReadFlag, 1));
    }

    @Override
    public Map<String, Object> center(Long userId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("unreadCount", unreadCount(userId));
        data.put("records", listUnreadFirst(userId));
        return data;
    }
}
