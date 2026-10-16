package vn.uet.auction.service;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

public final class PasswordUtil {
    private PasswordUtil() { }
    public static String newSalt() {
        byte[] data = new byte[16]; new SecureRandom().nextBytes(data);
        return Base64.getEncoder().encodeToString(data);
    }
    public static String hash(String password, String salt) {
        try {
            char[] chars = password.toCharArray();
            PBEKeySpec spec = new PBEKeySpec(chars, Base64.getDecoder().decode(salt), 120_000, 256);
            try {
                return Base64.getEncoder().encodeToString(SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded());
            } finally { spec.clearPassword(); java.util.Arrays.fill(chars, '\0'); }
        } catch (Exception e) { throw new IllegalStateException(e); }
    }
    public static boolean verify(String password, String salt, String expected) {
        return java.security.MessageDigest.isEqual(hash(password,salt).getBytes(java.nio.charset.StandardCharsets.UTF_8), expected.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }
}
