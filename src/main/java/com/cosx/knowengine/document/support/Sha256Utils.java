package com.cosx.knowengine.document.support;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

public final class Sha256Utils {

    private Sha256Utils() {
    }

    public static String digest(InputStream inputStream) throws IOException {
        MessageDigest digest = newDigest();
        byte[] buffer = new byte[8192];
        int length;
        while ((length = inputStream.read(buffer)) != -1) {
            digest.update(buffer, 0, length);
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    public static String digest(String value) {
        MessageDigest digest = newDigest();
        return HexFormat.of().formatHex(digest.digest(value.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
    }

    private static MessageDigest newDigest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("JDK does not provide SHA-256", exception);
        }
    }
}
