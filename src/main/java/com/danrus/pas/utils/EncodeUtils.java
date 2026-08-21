package com.danrus.pas.utils;

import com.danrus.pas.PlayerArmorStandsClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Locale;

public class EncodeUtils {
    public static String decodeBase64(String base64) {
        return new String(Base64.getDecoder().decode(base64), StandardCharsets.UTF_8);
    }

    public static String encodeToSha256(String source) {
        try {
            source = source.toLowerCase(Locale.ROOT);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (Exception e) {
            PlayerArmorStandsClient.LOGGER.warn("Failed to encode to SHA-256: {}", source, e);
            return source;
        }
    }
}
