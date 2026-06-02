package com.student.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.bank.entity.BankMessageOutbox;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BankMessageOutboxMapper extends BaseMapper<BankMessageOutbox> {
}
