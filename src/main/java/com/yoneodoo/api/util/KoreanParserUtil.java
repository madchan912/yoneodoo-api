package com.yoneodoo.api.util;

/**
 * 한글 재료명 검색을 돕기 위한 문자열 변환 유틸리티입니다.
 * <p>
 * <b>사용처</b><br>
 * {@link com.yoneodoo.api.service.IngredientSearchService}가 재료 캐시를 만들 때,
 * 각 재료명에 대해 "초성만 모은 문자열"과 "완전 자모 분해 문자열"을 미리 계산해 둡니다.
 * 사용자가 초성만 입력했는지(ㄱㄴㄷ) 또는 완성형을 입력했는지에 따라 검색 방식을 바꿉니다.
 * <p>
 * <b>한글 완성형 범위</b><br>
 * 유니코드 한글 음절 {@code 0xAC00}~{@code 0xD7A3}만 초/중/종성 분해 대상으로 보고,
 * 그 외 문자(영문·숫자 등)는 그대로 이어 붙입니다.
 */
public class KoreanParserUtil {
    /** 초성 테이블(한글 음절에서 뽑아낸 첫 자모). */
    private static final String[] CHO = {
            "ㄱ", "ㄲ", "ㄴ", "ㄷ", "ㄸ", "ㄹ", "ㅁ", "ㅂ", "ㅃ",
            "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅉ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    /**
     * 중성(모음) 테이블.
     * 이중 모음도 타이핑 순서대로 풀어 쓰기 위해 "ㅗㅏ"처럼 분리된 항목을 포함합니다.
     */
    private static final String[] JUNG = {
            "ㅏ", "ㅐ", "ㅑ", "ㅒ", "ㅓ", "ㅔ", "ㅕ", "ㅖ", "ㅗ", "ㅗㅏ",
            "ㅗㅐ", "ㅗㅣ", "ㅛ", "ㅜ", "ㅜㅓ", "ㅜㅔ", "ㅜㅣ", "ㅠ", "ㅡ", "ㅡㅣ", "ㅣ"
    };

    /**
     * 종성(받침) 테이블.
     * 겹받침은 "ㄹㄱ"처럼 풀어 쓴 문자열로 넣어 두었습니다.
     */
    private static final String[] JONG = {
            "", "ㄱ", "ㄲ", "ㄱㅅ", "ㄴ", "ㄴㅈ", "ㄴㅎ", "ㄷ", "ㄹ", "ㄹㄱ", "ㄹㅁ", "ㄹㅂ",
            "ㄹㅅ", "ㄹㅌ", "ㄹㅍ", "ㄹㅎ", "ㅁ", "ㅂ", "ㅂㅅ", "ㅅ", "ㅆ", "ㅇ", "ㅈ", "ㅊ", "ㅋ", "ㅌ", "ㅍ", "ㅎ"
    };

    /**
     * 단어에서 초성만 뽑아 이어 붙입니다.
     * <p>
     * 예: "진간장" → "ㅈㄱㅈ" (초성 검색 필터에 사용)
     *
     * @param word 입력 문자열
     * @return 초성 시퀀스(비한글은 그대로 출력)
     */
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

    /**
     * 단어를 자모 단위로 완전 분해합니다.
     * <p>
     * 예: "닭" → "ㄷㅏㄹㄱ" (완성형 검색 시 키워드와 부분 일치 비교에 사용)
     *
     * @param word 입력 문자열
     * @return 초성+중성(+종성)을 이어 붙인 자모열
     */
    public static String getJamo(String word) {
        if (word == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char c : word.toCharArray()) {
            if (c >= 0xAC00 && c <= 0xD7A3) {
                int cho = (c - 0xAC00) / 588;
                int jung = ((c - 0xAC00) % 588) / 28;
                int jong = (c - 0xAC00) % 28;

                sb.append(CHO[cho]).append(JUNG[jung]);
                if (jong > 0) sb.append(JONG[jong]);
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
