package com.student.bank.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class SecurityUtil {
    private static final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<String> WEAK_PASSWORDS = Set.of(
            "password", "password1", "qwerty123", "12345678", "abc123456", "11111111"
    );

    public static String encode(String rawPassword) {
        return encoder.encode(rawPassword);
    }

    public static boolean matches(String rawPassword, String encodedPassword) {
        if (rawPassword == null || encodedPassword == null) {
            return false;
        }
        return encoder.matches(rawPassword, encodedPassword);
    }

    public static boolean isStrongLoginPassword(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < 8 || rawPassword.length() > 32) {
            return false;
        }
        String lower = rawPassword.toLowerCase(Locale.ROOT);
        if (WEAK_PASSWORDS.contains(lower)) {
            return false;
        }
        boolean hasLetter = rawPassword.matches(".*[A-Za-z].*");
        boolean hasDigit = rawPassword.matches(".*\\d.*");
        boolean hasSpecial = rawPassword.matches(".*[^A-Za-z0-9].*");
        return hasLetter && hasDigit && hasSpecial;
    }

    public static boolean isTradePassword(String rawPassword) {
        return rawPassword != null && rawPassword.matches("^\\d{6}$");
    }

    public static BigDecimal normalizeMoney(BigDecimal amount) {
        if (amount == null) {
            return null;
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    public static boolean hasAtMostTwoDecimals(BigDecimal amount) {
        if (amount == null) {
            return false;
        }
        return amount.stripTrailingZeros().scale() <= 2;
    }

    public static String generateAccountNumber() {
        String prefix = "6217";
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyMMddHHmmss"));
        String suffix = String.format("%03d", RANDOM.nextInt(1000));
        return prefix + time + suffix;
    }

    public static String generateOrderNo(String prefix) {
        String time = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String suffix = String.format("%04d", RANDOM.nextInt(10000));
        return prefix + time + suffix;
    }

    public static String randomKey() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
