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
@TableName("bank_credit_card")
public class BankCreditCard {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long userId;
    private String cardNumber;
    private String productCode;
    private BigDecimal creditLimit;
    private BigDecimal usedAmount;
    private Integer points;
    private String status;
    private Integer billDay;
    private Integer repaymentDay;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
