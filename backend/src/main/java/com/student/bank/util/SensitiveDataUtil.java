package com.student.bank.util;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public final class SensitiveDataUtil {
    private static final String PREFIX = "v1:";
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String SECRET = System.getenv().getOrDefault("BANK_DATA_SECRET", "change-this-data-secret");

    private SensitiveDataUtil() {
    }

    public static String encrypt(String plain) {
        if (plain == null || plain.isBlank()) {
            return plain;
        }
        try {
            byte[] iv = new byte[12];
            RANDOM.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(aesKey(), "AES"), new GCMParameterSpec(128, iv));
            byte[] encrypted = cipher.doFinal(plain.getBytes(StandardCharsets.UTF_8));
            ByteBuffer buffer = ByteBuffer.allocate(iv.length + encrypted.length);
            buffer.put(iv).put(encrypted);
            return PREFIX + Base64.getEncoder().encodeToString(buffer.array());
        } catch (Exception e) {
            throw new IllegalStateException("敏感数据加密失败", e);
        }
    }

    public static String decrypt(String encrypted) {
        if (encrypted == null || encrypted.isBlank() || !encrypted.startsWith(PREFIX)) {
            return encrypted;
        }
        try {
            byte[] all = Base64.getDecoder().decode(encrypted.substring(PREFIX.length()));
            byte[] iv = new byte[12];
            byte[] data = new byte[all.length - 12];
            System.arraycopy(all, 0, iv, 0, 12);
            System.arraycopy(all, 12, data, 0, data.length);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(aesKey(), "AES"), new GCMParameterSpec(128, iv));
            return new String(cipher.doFinal(data), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new IllegalStateException("敏感数据解密失败", e);
        }
    }

    public static String hash(String value) {
        if (value == null) {
            return null;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return Base64.getUrlEncoder().withoutPadding()
                    .encodeToString(mac.doFinal(value.trim().toUpperCase().getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("敏感数据哈希失败", e);
        }
    }

    public static String maskAccount(String accountNumber) {
        if (accountNumber == null || accountNumber.length() < 8) {
            return accountNumber;
        }
        return accountNumber.substring(0, 4) + " **** **** " + accountNumber.substring(accountNumber.length() - 4);
    }

    public static String maskName(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }
        if (name.length() == 1) {
            return name + "*";
        }
        return name.charAt(0) + "*".repeat(Math.max(1, name.length() - 1));
    }

    public static String maskPhone(String phone) {
        String raw = decrypt(phone);
        if (raw == null || raw.length() < 7) {
            return raw;
        }
        return raw.substring(0, 3) + "****" + raw.substring(raw.length() - 4);
    }

    public static String maskIdCard(String idCard) {
        String raw = decrypt(idCard);
        if (raw == null || raw.length() < 8) {
            return raw;
        }
        return raw.substring(0, 4) + "**********" + raw.substring(raw.length() - 4);
    }

    private static byte[] aesKey() throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(SECRET.getBytes(StandardCharsets.UTF_8));
        byte[] key = new byte[16];
        System.arraycopy(hash, 0, key, 0, 16);
        return key;
    }
}
