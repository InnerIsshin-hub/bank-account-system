package com.student.bank.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bank_audit_log")
public class BankAuditLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String operationType;
    private String resourceType;
    private String resourceId;
    private String ip;
    private String userAgent;
    private String traceId;
    private String result;
    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
