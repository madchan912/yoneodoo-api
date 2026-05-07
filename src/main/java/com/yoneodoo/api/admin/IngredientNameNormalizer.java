package com.yoneodoo.api.admin;

/**
 * 재료 "이름" 문자열을 시스템 전역에서 같은 규칙으로 맞추기 위한 유틸리티입니다.
 * <p>
 * <b>왜 별도 클래스인가</b><br>
 * · 레시피 적재 시 {@link com.yoneodoo.api.service.RecipeService}는 공백을 제거합니다.<br>
 * · 어드민 매핑·미분류 집계는 이 클래스의 규칙을 사용합니다.<br>
 * 둘이 어긋나면 "같은 재료인데 미분류로 보인다" 같은 문제가 생기므로, 한곳에서 규칙을 정의하는 편이 안전합니다.
 * <p>
 * 현재 규칙: 앞뒤 trim + 중간 공백({@code " "}) 제거.
 */
public final class IngredientNameNormalizer {

    private IngredientNameNormalizer() {
    }

    /**
     * 비교·저장·검색 키로 쓸 정규화 문자열을 만듭니다.
     *
     * @param name 원본 재료명(레시피 JSON 또는 어드민 입력)
     * @return 정규화된 키( null 이면 빈 문자열 )
     */
    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().replace(" ", "");
    }
}
