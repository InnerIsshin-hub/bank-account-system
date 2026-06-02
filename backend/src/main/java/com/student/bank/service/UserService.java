package com.student.bank.service;

import com.student.bank.dto.LoginDTO;
import com.student.bank.dto.PasswordDTO;
import com.student.bank.dto.RegisterDTO;
import com.student.bank.dto.TradePasswordDTO;
import com.student.bank.dto.UpdateUserDTO;
import com.student.bank.dto.VerifyPasswordDTO;

import java.util.Map;

public interface UserService {
    String register(RegisterDTO dto);
    Map<String, Object> login(LoginDTO dto);
    Map<String, Object> me();
    void logout(String token);
    void updateProfile(UpdateUserDTO dto);
    void changePassword(PasswordDTO dto);
    void updateTradePassword(TradePasswordDTO dto);
    Map<String, Object> bindOtp(String loginPassword);
    void unbindOtp(String loginPassword, String otpCode);
    void closeAccount(String password);
    boolean verifyPassword(VerifyPasswordDTO dto);
    boolean verifyTradePassword(Long userId, String tradePassword);
}
