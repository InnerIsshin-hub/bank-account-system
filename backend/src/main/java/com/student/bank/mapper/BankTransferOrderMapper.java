package com.student.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.bank.entity.BankTransferOrder;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BankTransferOrderMapper extends BaseMapper<BankTransferOrder> {
}
