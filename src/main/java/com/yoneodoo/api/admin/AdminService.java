package com.yoneodoo.api.admin;

import com.yoneodoo.api.admin.dto.AdminDashboardStatsResponse;
import com.yoneodoo.api.admin.dto.AdminRecipeRowResponse;
import com.yoneodoo.api.admin.dto.UnclassifiedIngredientRowResponse;
import com.yoneodoo.api.entity.Recipe;
import com.yoneodoo.api.repository.RecipeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String STATUS_NO_SUBTITLES = "NO_SUBTITLES";
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final RecipeRepository recipeRepository;

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse dashboardStats() {
        long total = recipeRepository.count();
        long success = recipeRepository.countByStatus(STATUS_SUCCESS);
        long noSub = recipeRepository.countByStatus(STATUS_NO_SUBTITLES);
        long pending = recipeRepository.countPendingOrUnknown();
        long unclassified = 0L;
        return new AdminDashboardStatsResponse(total, success, noSub, pending, unclassified);
    }

    @Transactional(readOnly = true)
    public List<AdminRecipeRowResponse> listRecipesForAdmin(String filter) {
        String f = filter == null ? "all" : filter.trim().toLowerCase();
        List<Recipe> rows = switch (f) {
            case "pending" -> recipeRepository.findPendingForAdmin();
            case "no_subtitles", "nosubtitles" -> recipeRepository.findByStatus(STATUS_NO_SUBTITLES);
            default -> recipeRepository.findAll();
        };
        return rows.stream().map(this::toRow).toList();
    }

    @Transactional(readOnly = true)
    public List<UnclassifiedIngredientRowResponse> listUnclassifiedIngredients() {
        return List.of();
    }

    private AdminRecipeRowResponse toRow(Recipe r) {
        return new AdminRecipeRowResponse(
                r.getId(),
                r.getTitle(),
                r.getStatus(),
                r.getVideoId(),
                r.getYoutuberName(),
                r.getCreatedAt()
        );
    }
}
