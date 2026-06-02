package com.student.bank.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.student.bank.entity.BankAccount;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

@Mapper
public interface BankAccountMapper extends BaseMapper<BankAccount> {

    // 用于转账时的行锁查询（悲观锁）
    @Select("SELECT * FROM bank_account WHERE account_number = #{accountNumber} FOR UPDATE")
    BankAccount selectForUpdate(@Param("accountNumber") String accountNumber);

    @Update("UPDATE bank_account SET available_balance = available_balance + #{amount}, version = version + 1 WHERE account_number = #{accountNumber}")
    int updateBalance(@Param("accountNumber") String accountNumber, @Param("amount") BigDecimal amount);
}
