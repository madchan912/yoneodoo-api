package com.yoneodoo.api.admin.dto;

public record AdminDashboardStatsResponse(
        long totalRecipes,
        long successRecipes,
        long noSubtitlesRecipes,
        long pendingRecipes,
        long unclassifiedIngredients
) {
}
