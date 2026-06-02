package com.student.bank.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bank_user_security")
public class BankUserSecurity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String loginPasswordHash;
    private String tradePasswordHash;
    private Integer loginFailCount;
    private LocalDateTime lockedUntil;
    private Integer otpEnabled;
    private String otpSecret;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
