package com.student.bank.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("bank_batch_transfer_task")
public class BankBatchTransferTask {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String taskNo;
    private Long userId;
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private String status;
    private String detail;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
