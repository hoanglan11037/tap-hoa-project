package com.hoangdinh.taphoa.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/** Băm mật khẩu SHA-256 (hex thường), khớp với HASHBYTES('SHA2_256', ...) trong script CSDL. */
public final class PasswordHasher {

    private PasswordHasher() {
    }

    public static String hash(String plain) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(plain.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static boolean matches(String plain, String hashed) {
        return hashed != null && MessageDigest.isEqual(
                hash(plain).getBytes(StandardCharsets.US_ASCII), hashed.getBytes(StandardCharsets.US_ASCII));
    }
}
