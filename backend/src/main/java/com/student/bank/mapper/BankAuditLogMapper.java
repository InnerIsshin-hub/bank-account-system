package com.student.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.bank.entity.BankAuditLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BankAuditLogMapper extends BaseMapper<BankAuditLog> {
}
