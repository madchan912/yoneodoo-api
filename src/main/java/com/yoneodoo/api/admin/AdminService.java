package com.yoneodoo.api.admin;

import com.yoneodoo.api.admin.dto.AdminDashboardStatsResponse;
import com.yoneodoo.api.admin.dto.AdminRecipeRowResponse;
import com.yoneodoo.api.admin.dto.IngredientMappingRowResponse;
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

/**
 * 어드민(관리자) 화면 전용 비즈니스 로직입니다.
 * <p>
 * <b>다루는 데이터 범위</b><br>
 * · 레시피 테이블({@code recipes}) — 목록·건수 집계<br>
 * · 재료 매핑 테이블({@code ingredient_mapping}) — 미분류 분석, 매핑 저장/삭제<br>
 * · 재료 검색 캐시({@link IngredientSearchService}) — 매핑이 바뀌면 캐시를 다시 만들어 앱 검색 결과와 DB를 맞춤
 * <p>
 * 일반 사용자 API와 달리, 여기서는 "전체 레시피를 읽어 통계를 낸다" 같은 무거운 연산이 포함될 수 있습니다.
 */
@Service
@RequiredArgsConstructor
public class AdminService {

    /** 자막 없음 상태 코드 — 레시피 파이프라인에서 자막이 없을 때 저장되는 값. */
    private static final String STATUS_NO_SUBTITLES = "NO_SUBTITLES";
    /** 처리 완료(정상 종료)에 가까운 상태 코드. */
    private static final String STATUS_SUCCESS = "SUCCESS";

    /** 레시피 전체 조회·집계에 사용. */
    private final RecipeRepository recipeRepository;
    /** 재료 매핑 CRUD. */
    private final IngredientMappingRepository ingredientMappingRepository;
    /**
     * 매핑이 바뀌면 {@link IngredientSearchService#initCache()}를 호출해
     * "DB에 있는 재료명 집합"과 "검색 캐시"를 동기화합니다.
     */
    private final IngredientSearchService ingredientSearchService;

    /**
     * 어드민 대시보드 상단에 보여 줄 숫자 요약을 만듭니다.
     * <p>
     * 각 숫자의 의미:<br>
     * · total: 레시피 총건수<br>
     * · success: 상태가 SUCCESS인 건수<br>
     * · noSubtitles: 자막 없음(NO_SUBTITLES) 건수<br>
     * · pending: 아직 끝나지 않았거나 상태가 애매한 건수({@link RecipeRepository#countPendingOrUnknown()})<br>
     * · unclassified: "레시피 JSON에는 있는데 매핑 테이블에는 없는" 서로 다른 원재료 키 개수
     */
    @Transactional(readOnly = true)
    public AdminDashboardStatsResponse dashboardStats() {
        long total = recipeRepository.count();
        long success = recipeRepository.countByStatus(STATUS_SUCCESS);
        long noSub = recipeRepository.countByStatus(STATUS_NO_SUBTITLES);
        long pending = recipeRepository.countPendingOrUnknown();
        long unclassified = countDistinctUnclassifiedRawNames();
        return new AdminDashboardStatsResponse(total, success, noSub, pending, unclassified);
    }

    /**
     * 어드민 레시피 관리 화면용 목록입니다.
     *
     * @param filter {@code all} 전체, {@code pending} 처리 대기 느낌의 행만, {@code no_subtitles} 자막 없음만 등
     */
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

    /**
     * 이미 매핑 테이블에 올라가 있는 모든 행을 "최근 등록순"으로 돌려줍니다.
     * <p>
     * 정렬 기준: {@link IngredientMapping#getCreatedAt()} 내림차순(리포지토리 메서드 이름에 반영됨).
     */
    @Transactional(readOnly = true)
    public List<IngredientMappingRowResponse> listMappedIngredients() {
        return ingredientMappingRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(m -> new IngredientMappingRowResponse(
                        m.getRawName(),
                        m.getMasterName(),
                        m.getCreatedAt()
                ))
                .toList();
    }

    /**
     * 특정 원본 재료 키(raw)에 대한 매핑 한 줄을 DB에서 삭제합니다.
     * <p>
     * 흐름:<br>
     * 1) URL/파라미터로 들어온 문자열을 {@link IngredientNameNormalizer}로 정규화 → DB에 저장된 키와 동일한 규칙.<br>
     * 2) 해당 키의 행을 찾아 delete.<br>
     * 3) 성공 시 검색 캐시 재빌드({@link IngredientSearchService#initCache()}).<br>
     * 없으면 false를 반환하고, 컨트롤러에서 404로 바꿉니다.
     *
     * @param rawName 삭제할 원본 재료 표기(사용자 입력 그대로 가능 — 내부에서 정규화)
     * @return 삭제되면 true, 해당 키가 없으면 false
     */
    @Transactional
    public boolean deleteIngredientMappingByRawName(String rawName) {
        String key = IngredientNameNormalizer.normalize(rawName);
        if (!StringUtils.hasText(key)) {
            throw new IllegalArgumentException("rawName is empty after normalization");
        }
        return ingredientMappingRepository.findByRawName(key)
                .map(entity -> {
                    ingredientMappingRepository.delete(entity);
                    ingredientSearchService.initCache();
                    return true;
                })
                .orElse(false);
    }

    /**
     * "미분류" 재료 목록을 만듭니다.
     * <p>
     * 정의: <b>모든 레시피의 JSON 재료명을 정규화한 뒤 출현 횟수를 세고</b>,
     * 그 키가 {@code ingredient_mapping.raw_name}에 아직 없는 것만</b> 보여 줍니다.
     * <p>
     * 정렬: 출현 횟수 많은 순(운영에서 자주 나오는 미정리 재료부터 처리하기 좋게).
     */
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

    /**
     * 어드민이 여러 원본 재료를 한 마스터 이름 아래로 묶어 저장할 때 호출됩니다.
     * <p>
     * 처리 요약:<br>
     * · 마스터명·각 raw를 정규화해 빈 문자열을 걸러냄.<br>
     * · 각 raw에 대해 이미 행이 있으면 master만 갱신, 없으면 새 INSERT.<br>
     * · 끝나면 캐시 재빌드 후, 처리한 raw 개수를 반환합니다.
     *
     * @param request 마스터명 + 묶을 raw 이름 배열
     * @return 유효하게 처리 시도한 raw 개수(중복 제거 후)
     */
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

    /**
     * 대시보드의 "미분류 재료 종류 수"에 쓰는 내부 집계입니다.
     * {@link #collectNormalizedIngredientOccurrences()}의 키 중, 매핑 테이블에 없는 키만 센 개수입니다.
     */
    private long countDistinctUnclassifiedRawNames() {
        Map<String, Long> occ = collectNormalizedIngredientOccurrences();
        Set<String> mapped = ingredientMappingRepository.findAll().stream()
                .map(IngredientMapping::getRawName)
                .collect(Collectors.toSet());
        return occ.keySet().stream().filter(k -> !mapped.contains(k)).count();
    }

    /**
     * DB에 적재된 <b>모든 레시피</b>를 한 바퀴 돌며 재료명을 수집합니다.
     * <p>
     * 결과 맵의 의미:<br>
     * · key: {@link IngredientNameNormalizer}로 정규화한 재료명(서로 같은 재료로 취급할 식별자)<br>
     * · value: 그 이름이 레시피 JSON들에 등장한 총 횟수(여러 레시피·여러 번 포함)
     * <p>
     * 주의: 레시피 건수가 많으면 메모리·시간이 커지므로, 추후에는 배치/캐시/집계 테이블로 옮기는 개선 여지가 있습니다.
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

    /** 엔티티를 어드민 목록용 DTO로 가볍게 변환합니다(민감하거나 큰 필드는 제외). */
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
