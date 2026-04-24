package com.yoneodoo.api.admin;

/**
 * 레시피 적재 시 {@code RecipeService}와 동일하게 재료명에서 공백을 제거해 키를 맞춘다.
 */
public final class IngredientNameNormalizer {

    private IngredientNameNormalizer() {
    }

    public static String normalize(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().replace(" ", "");
    }
}
