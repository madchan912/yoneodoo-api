package com.yoneodoo.api.admin;

import com.yoneodoo.api.admin.dto.FoodSearchResponse;
import com.yoneodoo.api.admin.dto.NutritionStatsResponse;
import com.yoneodoo.api.admin.dto.NutritionUnmatchedResponse;
import com.yoneodoo.api.admin.dto.NutritionUpdateRequest;
import com.yoneodoo.api.entity.IngredientNutrition;
import com.yoneodoo.api.repository.FoodNutritionMasterRepository;
import com.yoneodoo.api.repository.IngredientNutritionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 어드민 영양성분 관리 서비스입니다.
 * <p>
 * <b>데이터 파이프라인 위치</b><br>
 * insert_nutrition.py로 자동 적재 후 남은 "manual_needed" 항목을
 * 어드민이 식품성분표를 검색·선택해 직접 채우는 흐름을 담당합니다.
 * <p>
 * 주요 흐름:
 * <ol>
 *   <li>미매칭 목록 조회 → ingredient_nutrition에서 source='manual_needed' 필터</li>
 *   <li>식품 검색 → food_nutrition_master에서 LIKE 검색(최대 20건)</li>
 *   <li>영양 값 저장 → ingredient_nutrition 업데이트, source를 'manual'로 변경</li>
 *   <li>통계 조회 → 전체/완료/미완료 카운트 반환</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class NutritionAdminService {

    private final IngredientNutritionRepository nutritionRepo;
    private final FoodNutritionMasterRepository foodMasterRepo;

    /**
     * ① source='manual_needed'인 재료 목록을 이름 오름차순으로 반환합니다.
     * 어드민이 수동으로 채워야 할 항목 목록에 사용합니다.
     */
    public List<NutritionUnmatchedResponse> listUnmatched() {
        return nutritionRepo.findBySourceOrderByMasterNameAsc("manual_needed")
                .stream()
                .map(n -> new NutritionUnmatchedResponse(n.getId(), n.getMasterName(), n.getSource()))
                .toList();
    }

    /**
     * ① 키워드로 food_nutrition_master를 LIKE 검색합니다(최대 20건).
     * 어드민이 직접 재료명을 입력해 식품성분표에서 적합한 항목을 찾는 데 사용합니다.
     */
    public List<FoodSearchResponse> searchFood(String keyword) {
        return foodMasterRepo.searchByFoodName(keyword, PageRequest.of(0, 20))
                .stream()
                .map(f -> new FoodSearchResponse(
                        f.getId(),
                        f.getFoodName(),
                        f.getFoodGroup(),
                        f.getCalories(),
                        f.getProtein(),
                        f.getFat(),
                        f.getSaturatedFat(),
                        f.getCarbohydrate(),
                        f.getSugar(),
                        f.getSodium()
                ))
                .toList();
    }

    /**
     * ① masterName으로 ingredient_nutrition을 찾고
     * ② 요청 본문의 영양 값과 source로 업데이트합니다.
     * ③ 저장 후 갱신된 항목을 반환합니다.
     *
     * @throws ResponseStatusException 404: masterName이 ingredient_nutrition에 없을 때
     */
    @Transactional
    public NutritionUnmatchedResponse updateNutrition(String masterName, NutritionUpdateRequest req) {
        IngredientNutrition n = nutritionRepo.findByMasterName(masterName)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "ingredient_nutrition not found: " + masterName));

        n.setCalories(req.calories());
        n.setProtein(req.protein());
        n.setFat(req.fat());
        n.setSaturatedFat(req.saturatedFat());
        n.setCarbohydrate(req.carbohydrate());
        n.setSugar(req.sugar());
        n.setSodium(req.sodium());
        n.setServingSize(BigDecimal.valueOf(100));
        n.setServingUnit("g");
        n.setSource(req.source() != null ? req.source() : "manual");
        n.setUpdatedAt(LocalDateTime.now());

        nutritionRepo.save(n);
        return new NutritionUnmatchedResponse(n.getId(), n.getMasterName(), n.getSource());
    }

    /**
     * ① 전체 행 수와 source별 카운트를 집계해 반환합니다.
     * 어드민 페이지 상단 요약 카드에 표시됩니다.
     */
    public NutritionStatsResponse getStats() {
        long total = nutritionRepo.count();
        long unmatched = nutritionRepo.countBySource("manual_needed");
        long matched = total - unmatched;
        return new NutritionStatsResponse(total, matched, unmatched);
    }
}
