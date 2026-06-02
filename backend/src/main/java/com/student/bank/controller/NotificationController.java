package com.student.bank.controller;

import com.student.bank.common.AuthContext;
import com.student.bank.common.Result;
import com.student.bank.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;

    @GetMapping
    public Result<Map<String, Object>> list() {
        return Result.success(notificationService.center(AuthContext.userId()));
    }

    @PutMapping("/{id}/read")
    public Result<Void> read(@PathVariable Long id) {
        notificationService.markRead(AuthContext.userId(), id);
        return Result.success("已标记为已读", null);
    }
}
