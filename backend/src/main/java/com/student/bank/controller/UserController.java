package com.student.bank.controller;

import com.student.bank.common.Result;
import com.student.bank.dto.LoginDTO;
import com.student.bank.dto.PasswordDTO;
import com.student.bank.dto.RegisterDTO;
import com.student.bank.dto.TradePasswordDTO;
import com.student.bank.dto.UpdateUserDTO;
import com.student.bank.dto.VerifyPasswordDTO;
import com.student.bank.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @PostMapping("/register")
    public Result<String> register(@Valid @RequestBody RegisterDTO dto) {
        String accountNumber = userService.register(dto);
        return Result.success("开户成功，您的卡号为：" + accountNumber, accountNumber);
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(userService.login(dto));
    }

    @PostMapping("/logout")
    public Result<Void> logout(HttpServletRequest request) {
        String auth = request.getHeader("Authorization");
        userService.logout(auth == null ? null : auth.replace("Bearer ", ""));
        return Result.success("退出成功", null);
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me() {
        return Result.success(userService.me());
    }

    @PutMapping("/profile")
    public Result<Void> updateProfile(@Valid @RequestBody UpdateUserDTO dto) {
        userService.updateProfile(dto);
        return Result.success("信息修改成功", null);
    }

    @PutMapping("/password")
    public Result<Void> changePassword(@Valid @RequestBody PasswordDTO dto) {
        userService.changePassword(dto);
        return Result.success("密码修改成功，请重新登录", null);
    }

    @PostMapping("/trade-password")
    public Result<Void> tradePassword(@Valid @RequestBody TradePasswordDTO dto) {
        userService.updateTradePassword(dto);
        return Result.success("交易密码设置成功", null);
    }

    @PostMapping("/otp/bind")
    public Result<Map<String, Object>> bindOtp(@RequestBody Map<String, String> body) {
        return Result.success(userService.bindOtp(body.get("loginPassword")));
    }

    @PostMapping("/otp/unbind")
    public Result<Void> unbindOtp(@RequestBody Map<String, String> body) {
        userService.unbindOtp(body.get("loginPassword"), body.get("otpCode"));
        return Result.success("OTP 已解绑", null);
    }

    @PostMapping("/verify-password")
    public Result<Boolean> verifyPassword(@Valid @RequestBody VerifyPasswordDTO dto) {
        return Result.success(userService.verifyPassword(dto));
    }

    @DeleteMapping("/close")
    public Result<Void> close(@RequestBody Map<String, String> body) {
        userService.closeAccount(body.get("password"));
        return Result.success("销户成功", null);
    }
}
