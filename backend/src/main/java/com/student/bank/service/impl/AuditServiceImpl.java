package com.student.bank.service.impl;

import com.student.bank.common.AuthContext;
import com.student.bank.common.AuthUser;
import com.student.bank.entity.BankAuditLog;
import com.student.bank.mapper.BankAuditLogMapper;
import com.student.bank.service.AuditService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditServiceImpl implements AuditService {
    private final BankAuditLogMapper auditLogMapper;

    @Override
    public void record(String operationType, String resourceType, String resourceId, String result, String detail) {
        try {
            BankAuditLog logEntity = new BankAuditLog();
            AuthUser user = AuthContext.get();
            logEntity.setUserId(user == null ? null : user.getUserId());
            logEntity.setOperationType(operationType);
            logEntity.setResourceType(resourceType);
            logEntity.setResourceId(resourceId);
            logEntity.setResult(result);
            logEntity.setDetail(detail == null ? null : detail.substring(0, Math.min(500, detail.length())));
            logEntity.setTraceId(MDC.get("traceId"));
            HttpServletRequest request = currentRequest();
            if (request != null) {
                logEntity.setIp(resolveIp(request));
                logEntity.setUserAgent(trim(request.getHeader("User-Agent"), 255));
            }
            auditLogMapper.insert(logEntity);
        } catch (Exception e) {
            log.warn("audit log write failed: {}", e.getMessage());
        }
    }

    private HttpServletRequest currentRequest() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private String resolveIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String trim(String value, int max) {
        if (value == null || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }
}
