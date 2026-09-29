package com.shmuelzon.HomeAssistantFloorPlan;

import java.nio.charset.StandardCharsets;
import java.util.Locale;

public class Utils {
    private static final char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    public static String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int b = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[b >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[b & 0x0F];
        }
        return new String(hexChars);
    }

    /* Converts a user given name to one usable as a directory name, keeping unicode letters, e.g., "1st Floor" -> "1st_floor" */
    public static String normalizeName(String name) {
        if (name == null)
            return "";
        return name.toLowerCase(Locale.ROOT).replaceAll("[^\\p{L}\\p{M}\\p{N}]+", "_").replaceAll("^_+|_+$", "");
    }

    /* Percent-encodes a string the same way browsers encode the URL's fragment, as returned by location.hash */
    public static String percentEncode(String value) {
        StringBuilder encoded = new StringBuilder();
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            int c = b & 0xFF;
            if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || (c >= '0' && c <= '9') || c == '_' || c == '-')
                encoded.append((char)c);
            else
                encoded.append('%').append(HEX_ARRAY[c >>> 4]).append(HEX_ARRAY[c & 0x0F]);
        }
        return encoded.toString();
    }

    public static String yamlQuote(String value) {
        return "'" + value.replace("'", "''") + "'";
    }
};
