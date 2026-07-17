package com.yoneodoo.api.admin;

import com.yoneodoo.api.admin.dto.FoodSearchResponse;
import com.yoneodoo.api.admin.dto.NutritionMatchedResponse;
import com.yoneodoo.api.admin.dto.NutritionStatsResponse;
import com.yoneodoo.api.admin.dto.NutritionUnmatchedResponse;
import com.yoneodoo.api.admin.dto.NutritionUpdateRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 어드민 영양성분 관리 REST 컨트롤러입니다.
 * <p>
 * {@code /api/v1/admin/**} 경로는 {@code AdminSecretAuthFilter}가 X-Admin-Secret 헤더를 검사합니다.
 * HTTP ↔ {@link NutritionAdminService} 사이 변환만 담당하며 비즈니스 로직은 서비스에 위임합니다.
 */
@RestController
@RequestMapping("/api/v1/admin/nutrition")
@RequiredArgsConstructor
public class NutritionAdminController {

    private final NutritionAdminService nutritionAdminService;

    /**
     * 적재 완료(source != 'manual_needed') 재료 목록을 반환합니다.
     * 어드민 완료 탭에서 기존 값 확인·수정 시 사용됩니다.
     */
    @GetMapping("/matched")
    public List<NutritionMatchedResponse> listMatched() {
        return nutritionAdminService.listMatched();
    }

    /**
     * 수동 입력이 필요한(source='manual_needed') 재료 목록을 반환합니다.
     * 어드민 왼쪽 패널의 미매칭 재료 목록에 사용됩니다.
     */
    @GetMapping("/unmatched")
    public List<NutritionUnmatchedResponse> listUnmatched() {
        return nutritionAdminService.listUnmatched();
    }

    /**
     * source='manual_needed' 재료 목록을 반환합니다.
     * 어드민 "확인필요" 탭 및 파이프라인 자동화에서 사용합니다.
     */
    @GetMapping("/manual-needed")
    public List<NutritionUnmatchedResponse> listManualNeeded() {
        return nutritionAdminService.listManualNeeded();
    }

    /**
     * 식품성분표(food_nutrition_master)에서 키워드로 식품명을 검색합니다(최대 20건).
     * 어드민이 수동 매칭 시 원하는 식품을 찾을 때 사용합니다.
     *
     * @param keyword 검색 키워드(예: "닭고기")
     */
    @GetMapping("/search")
    public List<FoodSearchResponse> searchFood(@RequestParam String keyword) {
        return nutritionAdminService.searchFood(keyword);
    }

    /**
     * masterName에 해당하는 ingredient_nutrition의 영양 값을 저장합니다.
     * 어드민이 식품을 선택·확인 후 저장 버튼을 누를 때 호출됩니다.
     *
     * @param masterName URL 인코딩된 표준 재료명(예: "닭고기")
     */
    @PutMapping("/{masterName}")
    public NutritionUnmatchedResponse updateNutrition(
            @PathVariable String masterName,
            @RequestBody NutritionUpdateRequest body) {
        return nutritionAdminService.updateNutrition(masterName, body);
    }

    /**
     * 전체/완료/미완료 카운트를 반환합니다.
     * 어드민 페이지 상단 통계 카드에 표시됩니다.
     */
    @GetMapping("/stats")
    public NutritionStatsResponse getStats() {
        return nutritionAdminService.getStats();
    }
}
