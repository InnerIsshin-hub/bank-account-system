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
@TableName("bank_transaction_record")
public class BankTransactionRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String recordNo;
    private String orderNo;
    private Long userId;
    private String accountNumber;
    private String direction;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String counterpartyAccount;
    private String counterpartyName;
    private String transactionType;
    private String category;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
