package com.yoneodoo.api.util;

public class KoreanParserUtil {
    private static final String[] CHO = {
            "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ",
            "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };
    private static final String[] JUNG = {
            "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅘ",
            "ㅙ", "ㅚ", "ㅛ", "ㅜ", "ㅝ", "ㅞ", "ㅟ", "ㅠ", "ㅡ", "ㅢ", "ㅣ"
    };
    private static final String[] JONG = {
            "", "ㄱ", "ㄲ", "ㄳ", "ㄴ", "ㄵ", "ㄶ", "ㄷ", "ㄹ", "ㄺ", "ㄻ", "ㄼ",
            "ㄽ", "ㄾ", "ㄿ", "ㅀ", "ㅁ", "ㅂ", "ㅄ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    // 1. 초성만 추출 (예: "진간장" -> "ㅈㄱㅈ")
    public static String getChosung(String word) {
        if (word == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                sb.append(CHO[(c - 0xAC00) / 588]);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    // 2. 자음/모음 완전 분리 (예: "진간장" -> "ㅈㅣㄴㄱㅏㄴㅈㅏㅇ")
    public static String getJamo(String word) {
        if (word == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                int cho = (c - 0xAC00) / 588;
                int jung = ((c - 0xAC00) % 588) / 28;
                int jong = (c - 0xAC00) % 28;

                sb.append(CHO[cho]).append(JUNG[jung]);
                if (jong > 0) sb.append(JONG[jong]); // 받침이 있으면 추가
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}