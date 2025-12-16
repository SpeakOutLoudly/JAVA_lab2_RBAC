package com.study.security;

/**
 * Utility for masking sensitive data in logs
 */
public class DataMasker {
    
    /**
     * Mask username - show first and last char only
     */
    public static String maskUsername(String username) {
        if (username == null || username.length() <= 2) {
            return "***";
        }
        return username.charAt(0) + "***" + username.charAt(username.length() - 1);
    }

    /**
     * Mask password completely
     */
    public static String maskPassword(String password) {
        return "******";
    }

    /**
     * Mask email - show first char and domain
     */
    public static String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***";
        }
        int atIndex = email.indexOf("@");
        return email.charAt(0) + "***" + email.substring(atIndex);
    }
}
