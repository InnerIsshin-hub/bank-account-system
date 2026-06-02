package com.student.bank.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.student.bank.common.AuthContext;
import com.student.bank.common.BusinessException;
import com.student.bank.common.ErrorCode;
import com.student.bank.dto.LoginDTO;
import com.student.bank.dto.PasswordDTO;
import com.student.bank.dto.RegisterDTO;
import com.student.bank.dto.TradePasswordDTO;
import com.student.bank.dto.UpdateUserDTO;
import com.student.bank.dto.VerifyPasswordDTO;
import com.student.bank.entity.BankAccount;
import com.student.bank.entity.BankKycRecord;
import com.student.bank.entity.BankPasswordHistory;
import com.student.bank.entity.BankUser;
import com.student.bank.entity.BankUserSecurity;
import com.student.bank.mapper.BankAccountMapper;
import com.student.bank.mapper.BankKycRecordMapper;
import com.student.bank.mapper.BankPasswordHistoryMapper;
import com.student.bank.mapper.BankUserMapper;
import com.student.bank.mapper.BankUserSecurityMapper;
import com.student.bank.service.AuditService;
import com.student.bank.service.NotificationService;
import com.student.bank.service.TokenBlacklistService;
import com.student.bank.service.UserService;
import com.student.bank.util.JwtUtil;
import com.student.bank.util.SecurityUtil;
import com.student.bank.util.SensitiveDataUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final BankUserMapper userMapper;
    private final BankUserSecurityMapper securityMapper;
    private final BankPasswordHistoryMapper passwordHistoryMapper;
    private final BankAccountMapper accountMapper;
    private final BankKycRecordMapper kycRecordMapper;
    private final JwtUtil jwtUtil;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuditService auditService;
    private final NotificationService notificationService;
    private final RateLimitService rateLimitService;

    @Value("${bank.security.login-max-failures:5}")
    private int maxLoginFailures;

    @Value("${bank.security.login-lock-minutes:10}")
    private long loginLockMinutes;

    @Value("${bank.jwt.access-token-minutes:120}")
    private long accessTokenMinutes;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String register(RegisterDTO dto) {
        if (!SecurityUtil.isStrongLoginPassword(dto.getPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "登录密码需 8-32 位，且包含字母、数字和特殊字符");
        }
        if (!SecurityUtil.isTradePassword(dto.getTradePassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "交易密码必须为 6 位数字");
        }
        if (dto.getPassword().equals(dto.getTradePassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "交易密码不能与登录密码相同");
        }

        String idHash = SensitiveDataUtil.hash(dto.getIdCard());
        String phoneHash = SensitiveDataUtil.hash(dto.getPhone());
        if (userMapper.selectCount(new LambdaQueryWrapper<BankUser>().eq(BankUser::getIdCardHash, idHash)) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该身份证已注册");
        }
        if (userMapper.selectCount(new LambdaQueryWrapper<BankUser>().eq(BankUser::getPhoneHash, phoneHash)) > 0) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "该手机号已注册");
        }

        BankUser user = new BankUser();
        user.setUserName(dto.getUserName().trim());
        user.setIdCard(SensitiveDataUtil.encrypt(dto.getIdCard().trim()));
        user.setIdCardHash(idHash);
        user.setPhone(SensitiveDataUtil.encrypt(dto.getPhone().trim()));
        user.setPhoneHash(phoneHash);
        user.setStatus("NORMAL");
        user.setKycStatus("VERIFIED");
        user.setRole("USER");
        user.setTokenVersion(0);
        user.setDeleted(0);
        userMapper.insert(user);

        BankUserSecurity security = new BankUserSecurity();
        security.setUserId(user.getId());
        security.setLoginPasswordHash(SecurityUtil.encode(dto.getPassword()));
        security.setTradePasswordHash(SecurityUtil.encode(dto.getTradePassword()));
        security.setLoginFailCount(0);
        security.setOtpEnabled(0);
        securityMapper.insert(security);
        savePasswordHistory(user.getId(), security.getLoginPasswordHash());

        BankKycRecord kyc = new BankKycRecord();
        kyc.setUserId(user.getId());
        kyc.setIdCardHash(idHash);
        kyc.setChannel("MANUAL_DEMO");
        kyc.setStatus("VERIFIED");
        kyc.setFaceResult("SIMULATED_PASS");
        kyc.setFaceScore(BigDecimal.valueOf(0.98));
        kyc.setReviewedAt(LocalDateTime.now());
        kycRecordMapper.insert(kyc);

        String accountNumber = createDefaultAccount(user.getId());
        auditService.record("REGISTER", "USER", String.valueOf(user.getId()), "SUCCESS", "开户成功");
        notificationService.notify(user.getId(), "开户成功", "您的本行账户已开通，卡号尾号 " + accountNumber.substring(accountNumber.length() - 4), "ACCOUNT_OPENED");
        return accountNumber;
    }

    @Override
    public Map<String, Object> login(LoginDTO dto) {
        rateLimitService.check("login:" + SensitiveDataUtil.hash(dto.getIdCard()), 10, Duration.ofMinutes(1));
        String idHash = SensitiveDataUtil.hash(dto.getIdCard());
        BankUser user = userMapper.selectOne(new LambdaQueryWrapper<BankUser>().eq(BankUser::getIdCardHash, idHash));
        if (user == null) {
            auditService.record("LOGIN", "USER", null, "FAIL", "身份或密码错误");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "身份证号或密码错误");
        }
        BankUserSecurity security = securityOf(user.getId());
        if (security.getLockedUntil() != null && security.getLockedUntil().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.ACCOUNT_LOCKED, "登录失败次数过多，请稍后再试");
        }
        if (!SecurityUtil.matches(dto.getPassword(), security.getLoginPasswordHash())) {
            int failCount = security.getLoginFailCount() == null ? 1 : security.getLoginFailCount() + 1;
            security.setLoginFailCount(failCount);
            if (failCount >= maxLoginFailures) {
                security.setLockedUntil(LocalDateTime.now().plusMinutes(loginLockMinutes));
            }
            securityMapper.updateById(security);
            auditService.record("LOGIN", "USER", String.valueOf(user.getId()), "FAIL", "身份或密码错误");
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "身份证号或密码错误");
        }
        if (!"NORMAL".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "该用户状态不允许登录");
        }

        security.setLoginFailCount(0);
        security.setLockedUntil(null);
        securityMapper.updateById(security);

        String token = jwtUtil.createToken(user.getId(), user.getUserName(), user.getRole(), user.getTokenVersion());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("accessToken", token);
        data.put("tokenType", "Bearer");
        data.put("expiresIn", accessTokenMinutes * 60);
        data.put("user", userView(user));
        data.put("accounts", accountViews(user.getId(), false));
        auditService.record("LOGIN", "USER", String.valueOf(user.getId()), "SUCCESS", "登录成功");
        return data;
    }

    @Override
    public Map<String, Object> me() {
        BankUser user = requireCurrentUser();
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", userView(user));
        data.put("accounts", accountViews(user.getId(), false));
        data.put("unreadCount", notificationService.unreadCount(user.getId()));
        return data;
    }

    @Override
    public void logout(String token) {
        if (token != null && !token.isBlank()) {
            tokenBlacklistService.add(token, jwtUtil.getExpireEpochSecond(token));
        }
        auditService.record("LOGOUT", "USER", String.valueOf(AuthContext.userId()), "SUCCESS", "退出登录");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateProfile(UpdateUserDTO dto) {
        BankUser user = requireCurrentUser();
        BankUserSecurity security = securityOf(user.getId());
        if (!SecurityUtil.matches(dto.getOldPassword(), security.getLoginPasswordHash())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "登录密码验证失败");
        }
        if (dto.getNewPhone() != null && !dto.getNewPhone().isBlank()) {
            String phoneHash = SensitiveDataUtil.hash(dto.getNewPhone());
            Long count = userMapper.selectCount(new LambdaQueryWrapper<BankUser>()
                    .eq(BankUser::getPhoneHash, phoneHash)
                    .ne(BankUser::getId, user.getId()));
            if (count > 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "手机号已被占用");
            }
            user.setPhone(SensitiveDataUtil.encrypt(dto.getNewPhone()));
            user.setPhoneHash(phoneHash);
            userMapper.updateById(user);
            auditService.record("UPDATE_PHONE", "USER", String.valueOf(user.getId()), "SUCCESS", "修改手机号");
        }
        if (dto.getNewPassword() != null && !dto.getNewPassword().isBlank()) {
            PasswordDTO passwordDTO = new PasswordDTO();
            passwordDTO.setOldPassword(dto.getOldPassword());
            passwordDTO.setNewPassword(dto.getNewPassword());
            changePassword(passwordDTO);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changePassword(PasswordDTO dto) {
        BankUser user = requireCurrentUser();
        BankUserSecurity security = securityOf(user.getId());
        if (!SecurityUtil.matches(dto.getOldPassword(), security.getLoginPasswordHash())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "旧密码错误");
        }
        if (!SecurityUtil.isStrongLoginPassword(dto.getNewPassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码需 8-32 位，且包含字母、数字和特殊字符");
        }
        List<BankPasswordHistory> histories = passwordHistoryMapper.selectList(new LambdaQueryWrapper<BankPasswordHistory>()
                .eq(BankPasswordHistory::getUserId, user.getId())
                .orderByDesc(BankPasswordHistory::getCreatedAt)
                .last("LIMIT 5"));
        for (BankPasswordHistory history : histories) {
            if (SecurityUtil.matches(dto.getNewPassword(), history.getPasswordHash())) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "新密码不能与最近使用过的密码相同");
            }
        }
        security.setLoginPasswordHash(SecurityUtil.encode(dto.getNewPassword()));
        securityMapper.updateById(security);
        savePasswordHistory(user.getId(), security.getLoginPasswordHash());

        user.setTokenVersion(user.getTokenVersion() + 1);
        userMapper.updateById(user);
        auditService.record("CHANGE_PASSWORD", "USER", String.valueOf(user.getId()), "SUCCESS", "修改登录密码");
        notificationService.notify(user.getId(), "登录密码已修改", "如非本人操作，请立即联系银行客服。", "PASSWORD_CHANGED");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateTradePassword(TradePasswordDTO dto) {
        BankUser user = requireCurrentUser();
        BankUserSecurity security = securityOf(user.getId());
        if (!SecurityUtil.matches(dto.getLoginPassword(), security.getLoginPasswordHash())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "登录密码验证失败");
        }
        if (dto.getOldTradePassword() != null && !dto.getOldTradePassword().isBlank()
                && !SecurityUtil.matches(dto.getOldTradePassword(), security.getTradePasswordHash())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "原交易密码错误");
        }
        if (!SecurityUtil.isTradePassword(dto.getNewTradePassword())) {
            throw new BusinessException(ErrorCode.PARAM_ERROR, "交易密码必须为 6 位数字");
        }
        security.setTradePasswordHash(SecurityUtil.encode(dto.getNewTradePassword()));
        securityMapper.updateById(security);
        auditService.record("CHANGE_TRADE_PASSWORD", "USER_SECURITY", String.valueOf(user.getId()), "SUCCESS", "设置交易密码");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> bindOtp(String loginPassword) {
        BankUser user = requireCurrentUser();
        BankUserSecurity security = securityOf(user.getId());
        if (!SecurityUtil.matches(loginPassword, security.getLoginPasswordHash())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "登录密码验证失败");
        }
        String secret = "DEMO-OTP-" + user.getId() + "-" + System.currentTimeMillis();
        security.setOtpEnabled(1);
        security.setOtpSecret(secret);
        securityMapper.updateById(security);
        auditService.record("BIND_OTP", "USER_SECURITY", String.valueOf(user.getId()), "SUCCESS", "绑定 OTP");
        notificationService.notify(user.getId(), "OTP 已绑定", "您的动态口令已启用，高风险交易将要求增强验证。", "OTP_BOUND");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("otpEnabled", true);
        data.put("secret", secret);
        data.put("demoCode", "000000");
        return data;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbindOtp(String loginPassword, String otpCode) {
        BankUser user = requireCurrentUser();
        BankUserSecurity security = securityOf(user.getId());
        if (!SecurityUtil.matches(loginPassword, security.getLoginPasswordHash())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "登录密码验证失败");
        }
        if (!"000000".equals(otpCode)) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "OTP 验证码错误，demo 验证码为 000000");
        }
        security.setOtpEnabled(0);
        security.setOtpSecret(null);
        securityMapper.updateById(security);
        auditService.record("UNBIND_OTP", "USER_SECURITY", String.valueOf(user.getId()), "SUCCESS", "解绑 OTP");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeAccount(String password) {
        BankUser user = requireCurrentUser();
        BankUserSecurity security = securityOf(user.getId());
        if (!SecurityUtil.matches(password, security.getLoginPasswordHash())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "密码验证失败");
        }
        List<BankAccount> accounts = accountMapper.selectList(new LambdaQueryWrapper<BankAccount>()
                .eq(BankAccount::getUserId, user.getId()));
        for (BankAccount account : accounts) {
            if (account.getAvailableBalance().compareTo(BigDecimal.ZERO) != 0
                    || account.getFrozenBalance().compareTo(BigDecimal.ZERO) != 0) {
                throw new BusinessException(ErrorCode.PARAM_ERROR, "所有关联账户余额必须为 0 才能销户");
            }
        }
        user.setStatus("CLOSED");
        user.setTokenVersion(user.getTokenVersion() + 1);
        userMapper.updateById(user);
        accountMapper.update(null, new LambdaUpdateWrapper<BankAccount>()
                .eq(BankAccount::getUserId, user.getId())
                .set(BankAccount::getStatus, "CLOSED")
                .set(BankAccount::getClosedAt, LocalDateTime.now()));
        auditService.record("CLOSE_USER", "USER", String.valueOf(user.getId()), "SUCCESS", "用户销户");
    }

    @Override
    public boolean verifyPassword(VerifyPasswordDTO dto) {
        BankUser user = requireCurrentUser();
        boolean trade = "trade".equalsIgnoreCase(dto.getType());
        boolean valid = trade ? verifyTradePassword(user.getId(), dto.getPassword())
                : SecurityUtil.matches(dto.getPassword(), securityOf(user.getId()).getLoginPasswordHash());
        auditService.record(trade ? "VERIFY_TRADE_PASSWORD" : "VERIFY_LOGIN_PASSWORD", "USER", String.valueOf(user.getId()),
                valid ? "SUCCESS" : "FAIL", "敏感操作二次验证");
        return valid;
    }

    @Override
    public boolean verifyTradePassword(Long userId, String tradePassword) {
        return SecurityUtil.matches(tradePassword, securityOf(userId).getTradePasswordHash());
    }

    private String createDefaultAccount(Long userId) {
        for (int i = 0; i < 8; i++) {
            String accountNumber = SecurityUtil.generateAccountNumber();
            if (accountMapper.selectCount(new LambdaQueryWrapper<BankAccount>()
                    .eq(BankAccount::getAccountNumber, accountNumber)) == 0) {
                BankAccount account = new BankAccount();
                account.setAccountNumber(accountNumber);
                account.setUserId(userId);
                account.setAccountType("CURRENT");
                account.setStatus("NORMAL");
                account.setCurrency("CNY");
                account.setAvailableBalance(BigDecimal.valueOf(10000).setScale(2));
                account.setFrozenBalance(BigDecimal.ZERO.setScale(2));
                account.setVersion(0);
                account.setOpenTime(LocalDateTime.now());
                account.setDeleted(0);
                accountMapper.insert(account);
                return accountNumber;
            }
        }
        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "生成唯一卡号失败，请稍后重试");
    }

    private BankUser requireCurrentUser() {
        BankUser user = userMapper.selectById(AuthContext.userId());
        if (user == null || !"NORMAL".equals(user.getStatus())) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        return user;
    }

    private BankUserSecurity securityOf(Long userId) {
        BankUserSecurity security = securityMapper.selectOne(new LambdaQueryWrapper<BankUserSecurity>()
                .eq(BankUserSecurity::getUserId, userId));
        if (security == null) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "用户安全资料不存在");
        }
        return security;
    }

    private void savePasswordHistory(Long userId, String passwordHash) {
        BankPasswordHistory history = new BankPasswordHistory();
        history.setUserId(userId);
        history.setPasswordHash(passwordHash);
        passwordHistoryMapper.insert(history);
    }

    private Map<String, Object> userView(BankUser user) {
        BankUserSecurity security = securityOf(user.getId());
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", user.getId());
        data.put("userName", user.getUserName());
        data.put("idCardMasked", SensitiveDataUtil.maskIdCard(user.getIdCard()));
        data.put("phoneMasked", SensitiveDataUtil.maskPhone(user.getPhone()));
        data.put("status", user.getStatus());
        data.put("kycStatus", user.getKycStatus());
        data.put("role", user.getRole());
        data.put("otpEnabled", security.getOtpEnabled() != null && security.getOtpEnabled() == 1);
        return data;
    }

    private List<Map<String, Object>> accountViews(Long userId, boolean fullCard) {
        return accountMapper.selectList(new LambdaQueryWrapper<BankAccount>()
                        .eq(BankAccount::getUserId, userId)
                        .orderByAsc(BankAccount::getId))
                .stream()
                .map(account -> {
                    Map<String, Object> data = new LinkedHashMap<>();
                    data.put("accountNumber", fullCard ? account.getAccountNumber() : SensitiveDataUtil.maskAccount(account.getAccountNumber()));
                    data.put("rawAccountNumber", account.getAccountNumber());
                    data.put("accountType", account.getAccountType());
                    data.put("status", account.getStatus());
                    data.put("currency", account.getCurrency());
                    data.put("availableBalance", account.getAvailableBalance());
                    data.put("frozenBalance", account.getFrozenBalance());
                    data.put("balance", account.getAvailableBalance());
                    data.put("openTime", account.getOpenTime());
                    return data;
                }).toList();
    }
}
