package com.yoneodoo.api.admin;

import com.yoneodoo.api.admin.dto.AdminDashboardStatsResponse;
import com.yoneodoo.api.admin.dto.AdminRecipeRowResponse;
import com.yoneodoo.api.admin.dto.IngredientMappingRowResponse;
import com.yoneodoo.api.admin.dto.IngredientMappingSaveRequest;
import com.yoneodoo.api.admin.dto.UnclassifiedIngredientRowResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

/**
 * 관리자(내부 운영)용 HTTP API 묶음입니다.
 * <p>
 * <b>보안</b><br>
 * 이 경로({@code /api/v1/admin/**})는 {@link com.yoneodoo.api.config.AdminSecretAuthFilter}에서
 * {@code X-Admin-Secret} 헤더를 검사합니다. 일반 사용자는 호출할 수 없게 설계합니다.
 * <p>
 * <b>비즈니스 로직 위치</b><br>
 * 이 클래스는 요청/응답 형식과 HTTP 상태만 다루고, 실제 DB·집계·캐시 갱신은 {@link AdminService}가 담당합니다.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@CrossOrigin(origins = {"http://localhost:5173", "https://yoneodoo.vercel.app"})
public class AdminController {

    private final AdminService adminService;

    /** 대시보드 숫자 카드용 집계(레시피 건수·미분류 재료 수 등). */
    @GetMapping("/dashboard/stats")
    public AdminDashboardStatsResponse dashboardStats() {
        return adminService.dashboardStats();
    }

    /**
     * 레시피 목록(필터: 전체/대기/자막없음 등).
     * 실제 SQL은 {@link com.yoneodoo.api.repository.RecipeRepository} 쪽에 정의된 메서드에 따릅니다.
     */
    @GetMapping("/recipes")
    public List<AdminRecipeRowResponse> listRecipes(
            @RequestParam(name = "filter", defaultValue = "all") String filter
    ) {
        return adminService.listRecipesForAdmin(filter);
    }

    /**
     * 아직 {@code ingredient_mapping}에 없는 재료 키만 모아, 출현 횟수와 함께 반환합니다.
     * (레시피 JSON 전체를 스캔하므로 데이터가 많으면 느려질 수 있습니다.)
     */
    @GetMapping("/ingredients/unclassified")
    public List<UnclassifiedIngredientRowResponse> unclassifiedIngredients() {
        return adminService.listUnclassifiedIngredients();
    }

    /** 매핑 테이블에 이미 있는 전체 행을 최신 등록순으로 반환합니다. */
    @GetMapping("/ingredients/mapped")
    public List<IngredientMappingRowResponse> mappedIngredients() {
        return adminService.listMappedIngredients();
    }

    /**
     * 한 건의 매핑을 삭제합니다.
     *
     * @param rawName 경로 변수 — URL 인코딩된 값이 디코딩된 뒤 서비스에서 다시 정규화됩니다.
     */
    @DeleteMapping("/ingredients/mapping/{rawName}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteIngredientMapping(@PathVariable String rawName) {
        try {
            boolean deleted = adminService.deleteIngredientMappingByRawName(rawName);
            if (!deleted) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "No mapping for rawName");
            }
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 여러 원본 재료를 하나의 마스터 이름 아래로 저장(또는 기존 행의 마스터만 변경)합니다.
     * 성공 시 {@code { "updated": 숫자 }} 형태로 몇 건을 처리했는지 돌려줍니다.
     */
    @PostMapping("/ingredients/mapping")
    public Map<String, Object> saveIngredientMapping(@RequestBody IngredientMappingSaveRequest body) {
        try {
            int updated = adminService.saveIngredientMappings(body);
            return Map.of("updated", updated);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
