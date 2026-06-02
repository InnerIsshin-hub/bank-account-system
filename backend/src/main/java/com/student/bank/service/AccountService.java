package com.student.bank.service;

import com.student.bank.dto.AccountApplyDTO;
import com.student.bank.dto.BatchTransferDTO;
import com.student.bank.dto.CloseAccountDTO;
import com.student.bank.dto.FreezeAccountDTO;
import com.student.bank.dto.TransferConfirmDTO;
import com.student.bank.dto.TransferDTO;
import com.student.bank.dto.TransferExecuteDTO;
import com.student.bank.dto.TransferPrecheckDTO;
import com.student.bank.entity.BankTransferOrder;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface AccountService {
    void deposit(String accountNumber, BigDecimal amount);
    void withdraw(String accountNumber, BigDecimal amount);
    Map<String, Object> transfer(TransferDTO dto);
    BigDecimal getBalance(String accountNumber);
    List<Map<String, Object>> listAccounts();
    Map<String, Object> getAccountDetail(String accountNumber);
    Map<String, Object> applyAccount(AccountApplyDTO dto);
    void closeAccount(String accountNumber, CloseAccountDTO dto);
    void freezeAccount(String accountNumber, FreezeAccountDTO dto);
    void unfreezeAccount(String accountNumber, FreezeAccountDTO dto);
    Map<String, Object> precheck(TransferPrecheckDTO dto);
    Map<String, Object> confirmTransfer(TransferConfirmDTO dto);
    Map<String, Object> executeTransfer(TransferExecuteDTO dto);
    BankTransferOrder getOrder(String orderNo);
    Map<String, Object> batchPrecheck(BatchTransferDTO dto);
    Map<String, Object> batchExecute(BatchTransferDTO dto);
}
