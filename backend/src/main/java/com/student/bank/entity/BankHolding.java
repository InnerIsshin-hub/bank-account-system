package com.student.bank.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("bank_holding")
public class BankHolding {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String accountNumber;
    private String productCode;
    private String productType;
    private BigDecimal amount;
    private String status;
    private LocalDateTime startAt;
    private LocalDateTime maturityAt;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
