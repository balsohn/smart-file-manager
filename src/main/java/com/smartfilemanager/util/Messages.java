package com.smartfilemanager.util;

import java.util.Locale;
import java.util.ResourceBundle;

public class Messages {
    private static ResourceBundle bundle;

    static {
        try {
            // messages.properties 파일을 resources 루트에서 로드
            bundle = ResourceBundle.getBundle("messages", Locale.ENGLISH);
            System.out.println("✅ Messages.properties loaded successfully");
        } catch (Exception e) {
            System.err.println("❌ Failed to load messages.properties: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static String get(String key) {
        try {
            return bundle != null ? bundle.getString(key) : key;
        } catch (Exception e) {
            System.err.println("❌ Key not found: " + key);
            return key; // 키를 찾지 못하면 키 자체를 반환
        }
    }

    // 한글 문제 해결 후 사용할 메서드
    public static void switchToKorean() {
        try {
            bundle = ResourceBundle.getBundle("messages", Locale.KOREAN);
            System.out.println("✅ Switched to Korean messages");
        } catch (Exception e) {
            System.err.println("❌ Failed to switch to Korean: " + e.getMessage());
        }
    }
}