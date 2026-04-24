package com.yoneodoo.api.admin;

import com.yoneodoo.api.admin.dto.AdminDashboardStatsResponse;
import com.yoneodoo.api.admin.dto.AdminRecipeRowResponse;
import com.yoneodoo.api.admin.dto.IngredientMappingSaveRequest;
import com.yoneodoo.api.admin.dto.UnclassifiedIngredientRowResponse;
import com.yoneodoo.api.dto.RecipeIngredientData;
import com.yoneodoo.api.entity.IngredientMapping;
import com.yoneodoo.api.entity.Recipe;
import com.yoneodoo.api.repository.IngredientMappingRepository;
import com.yoneodoo.api.repository.RecipeRepository;
import com.yoneodoo.api.service.IngredientSearchService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminService {

    private static final String STATUS_NO_SUBTITLES = "NO_SUBTITLES";
    private static final String STATUS_SUCCESS = "SUCCESS";

    private final RecipeRepository recipeRepository;
    private final IngredientMappingRepository ingredientMappingRepository;
    private final IngredientSearchService ingredientSearchService;

    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse dashboardStats() {
        long total = recipeRepository.count();
        long success = recipeRepository.countByStatus(STATUS_SUCCESS);
        long noSub = recipeRepository.countByStatus(STATUS_NO_SUBTITLES);
        long pending = recipeRepository.countPendingOrUnknown();
        long unclassified = countDistinctUnclassifiedRawNames();
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
        Map<String, Long> occurrences = collectNormalizedIngredientOccurrences();
        Set<String> mappedRaws = ingredientMappingRepository.findAll().stream()
                .map(IngredientMapping::getRawName)
                .collect(Collectors.toSet());

        return occurrences.entrySet().stream()
                .filter(e -> !mappedRaws.contains(e.getKey()))
                .map(e -> new UnclassifiedIngredientRowResponse(e.getKey(), e.getValue()))
                .sorted(Comparator.comparingLong(UnclassifiedIngredientRowResponse::occurrenceCount).reversed())
                .toList();
    }

    @Transactional
    public int saveIngredientMappings(IngredientMappingSaveRequest request) {
        if (request == null || !StringUtils.hasText(request.getMasterName())) {
            throw new IllegalArgumentException("masterName is required");
        }
        if (request.getRawNames() == null || request.getRawNames().isEmpty()) {
            throw new IllegalArgumentException("rawNames must not be empty");
        }

        String master = IngredientNameNormalizer.normalize(request.getMasterName());
        if (master.isEmpty()) {
            throw new IllegalArgumentException("masterName is empty after normalization");
        }

        LinkedHashSet<String> uniqueRaws = new LinkedHashSet<>();
        for (String raw : request.getRawNames()) {
            String n = IngredientNameNormalizer.normalize(raw);
            if (!n.isEmpty()) {
                uniqueRaws.add(n);
            }
        }
        if (uniqueRaws.isEmpty()) {
            throw new IllegalArgumentException("no valid rawNames after normalization");
        }

        int updated = 0;
        for (String raw : uniqueRaws) {
            ingredientMappingRepository.findByRawName(raw).ifPresentOrElse(
                    existing -> {
                        existing.setMasterName(master);
                        ingredientMappingRepository.save(existing);
                    },
                    () -> ingredientMappingRepository.save(new IngredientMapping(raw, master))
            );
            updated++;
        }

        ingredientSearchService.initCache();
        return updated;
    }

    private long countDistinctUnclassifiedRawNames() {
        Map<String, Long> occ = collectNormalizedIngredientOccurrences();
        Set<String> mapped = ingredientMappingRepository.findAll().stream()
                .map(IngredientMapping::getRawName)
                .collect(Collectors.toSet());
        return occ.keySet().stream().filter(k -> !mapped.contains(k)).count();
    }

    /**
     * 레시피 JSON 전체에서 정규화된 재료명 → 출현 횟수.
     */
    private Map<String, Long> collectNormalizedIngredientOccurrences() {
        Map<String, Long> counts = new HashMap<>();
        for (Recipe recipe : recipeRepository.findAll()) {
            List<RecipeIngredientData> ings = recipe.getIngredients();
            if (ings == null) {
                continue;
            }
            for (RecipeIngredientData ing : ings) {
                String key = IngredientNameNormalizer.normalize(ing.getName());
                if (key.isEmpty()) {
                    continue;
                }
                counts.merge(key, 1L, Long::sum);
            }
        }
        return counts;
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
