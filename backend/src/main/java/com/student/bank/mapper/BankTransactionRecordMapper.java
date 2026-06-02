package com.student.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.bank.entity.BankTransactionRecord;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BankTransactionRecordMapper extends BaseMapper<BankTransactionRecord> {
}
