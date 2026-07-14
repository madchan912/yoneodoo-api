package com.yoneodoo.api.admin;

import com.yoneodoo.api.admin.dto.AdminDashboardStatsResponse;
import com.yoneodoo.api.admin.dto.CrawlHistoryResponse;
import com.yoneodoo.api.admin.dto.CrawlTriggerRequest;
import com.yoneodoo.api.admin.dto.AdminRecipeDetailResponse;
import com.yoneodoo.api.admin.dto.AdminRecipeRowResponse;
import com.yoneodoo.api.admin.dto.AdminRecipeUpdateRequest;
import com.yoneodoo.api.admin.dto.AdminTaskBoardResponse;
import com.yoneodoo.api.admin.dto.IngredientBulkMapRequest;
import com.yoneodoo.api.admin.dto.IngredientMappingRowResponse;
import com.yoneodoo.api.admin.dto.IngredientMappingSaveRequest;
import com.yoneodoo.api.admin.dto.IngredientSuggestionRequest;
import com.yoneodoo.api.admin.dto.IngredientSuggestionResponse;
import com.yoneodoo.api.admin.dto.UnclassifiedIngredientRecipeResponse;
import com.yoneodoo.api.admin.dto.UnclassifiedIngredientRowResponse;
import com.yoneodoo.api.admin.dto.WatchedYoutuberRequest;
import com.yoneodoo.api.admin.dto.WatchedYoutuberResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
public class AdminController {

    private final AdminService adminService;
    private final IngredientSuggestionService ingredientSuggestionService;
    private final IngredientBulkGroupingService ingredientBulkGroupingService;
    private final CrawlProxyService crawlProxyService;
    private final YoutuberService youtuberService;

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
     * 레시피 한 건의 상세 정보(편집 화면 진입 시 사용).
     */
    @GetMapping("/recipes/{id}")
    public AdminRecipeDetailResponse getRecipe(@PathVariable Long id) {
        AdminRecipeDetailResponse detail = adminService.getRecipeDetail(id);
        if (detail == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "recipe not found: " + id);
        }
        return detail;
    }

    /**
     * 레시피 한 건의 요리명·유튜브 URL·재료 배열을 수정합니다.
     * 성공 시 수정된 상세 DTO를 반환합니다.
     */
    @PutMapping("/recipes/{id}")
    public AdminRecipeDetailResponse updateRecipe(
            @PathVariable Long id,
            @RequestBody AdminRecipeUpdateRequest body
    ) {
        try {
            AdminRecipeDetailResponse updated = adminService.updateRecipe(id, body);
            if (updated == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "recipe not found: " + id);
            }
            return updated;
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 어드민 "로드맵" 화면용 — 프로젝트 루트의 {@code TASK.md} 원문을 읽어 그대로 돌려줍니다.
     * 파일이 없으면 404. 파일은 마크다운이며, 프런트엔드에서 렌더링합니다.
     */
    @GetMapping("/tasks")
    public AdminTaskBoardResponse getTaskBoard() {
        AdminTaskBoardResponse res = adminService.readTaskMarkdown();
        if (res == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "TASK.md not found");
        }
        return res;
    }

    /**
     * 아직 {@code ingredient_mapping}에 없는 재료 키만 모아, 출현 횟수와 함께 반환합니다.
     * (레시피 JSON 전체를 스캔하므로 데이터가 많으면 느려질 수 있습니다.)
     */
    @GetMapping("/ingredients/unclassified")
    public List<UnclassifiedIngredientRowResponse> unclassifiedIngredients() {
        return adminService.listUnclassifiedIngredients();
    }

    /**
     * 특정 미분류 재료명이 포함된 레시피 목록을 반환합니다.
     * <p>
     * 어드민이 rawName 을 마스터로 묶기 전에 "이 재료가 실제 어느 레시피에 쓰이는지" 확인하는 용도입니다.
     * rawName 은 서비스 내부에서 {@link com.yoneodoo.api.admin.IngredientNameNormalizer}로 정규화됩니다.
     *
     * @param rawName URL 경로로 전달된 원본 재료 표기 (URL 디코딩 후 전달)
     * @return 해당 재료가 포함된 레시피 요약 목록 (생성일 최신순)
     */
    @GetMapping("/ingredients/unclassified/{rawName}/recipes")
    public List<UnclassifiedIngredientRecipeResponse> listRecipesByRawName(@PathVariable String rawName) {
        try {
            return adminService.listRecipesByRawName(rawName);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /** 매핑 테이블에 이미 있는 전체 행을 최신 등록순으로 반환합니다. */
    @GetMapping("/ingredients/mapped")
    public List<IngredientMappingRowResponse> mappedIngredients() {
        return adminService.listMappedIngredients();
    }

    /**
     * 매핑 테이블에 등록된 raw_name 값만 목록으로 반환합니다.
     * <p>
     * RecipeEditModal 에서 각 재료명이 매핑되어 있는지 확인할 때 사용합니다.
     * (masterName·createdAt 등 불필요한 필드를 제외해 응답 크기를 최소화합니다.)
     */
    @GetMapping("/ingredients/mapped-names")
    public List<String> mappedIngredientNames() {
        return adminService.listMappedRawNames();
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

    /**
     * Gemini API 를 호출해 선택된 원본 재료들에 대한 마스터 재료명 한 단어를 추천받습니다.
     * <p>
     * 응답은 즉시 DB 에 반영되지 않고(프런트의 입력창에 자동 입력되기만 함),
     * 어드민이 [매핑 저장] 버튼을 눌러야 최종 저장됩니다(Human-in-the-Loop).
     * <p>
     * 실패 상태 코드 매핑:<br>
     * · {@code 400} — rawNames 가 비어 있음<br>
     * · {@code 502} — Gemini 4xx/5xx 응답 또는 응답 파싱 실패<br>
     * · {@code 503} — API 키 미설정 (운영에서 환경변수 누락 등)<br>
     * · {@code 504} — Gemini 호출 타임아웃/네트워크 오류
     */
    @PostMapping("/ingredients/suggest")
    public IngredientSuggestionResponse suggestIngredientMapping(@RequestBody IngredientSuggestionRequest body) {
        return ingredientSuggestionService.suggest(body == null ? null : body.getRawNames());
    }

    /**
     * DB 상 매핑되지 않은 모든 미분류 재료명을 Gemini 에게 보내 마스터 기준 그룹(JSON 객체)을 받습니다.
     * DB 저장 없음 — 프런트 승인 모달 후 {@link #bulkMapIngredients(IngredientBulkMapRequest)} 로 확정합니다.
     */
    @PostMapping("/ingredients/bulk-suggest")
    public Map<String, List<String>> bulkSuggestIngredientGroups() {
        return ingredientBulkGroupingService.suggestBulkGroupingForAllUnclassified();
    }

    // ───────────────── 유튜버 관리 ─────────────────

    /** 등록된 유튜버 목록(등록 최신순). 각 항목에 레시피 수 포함. */
    @GetMapping("/youtubers")
    public List<WatchedYoutuberResponse> listYoutubers() {
        return youtuberService.listYoutubers();
    }

    /**
     * 유튜버를 신규 등록합니다.
     *
     * @param body 채널 URL·표시명
     * @return 저장된 유튜버 DTO (201 Created)
     */
    @PostMapping("/youtubers")
    @ResponseStatus(HttpStatus.CREATED)
    public WatchedYoutuberResponse addYoutuber(@RequestBody WatchedYoutuberRequest body) {
        try {
            return youtuberService.addYoutuber(body);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    /**
     * 유튜버를 목록에서 삭제합니다. 크롤링 이력은 유지됩니다.
     *
     * @param id 삭제할 유튜버 PK
     */
    @DeleteMapping("/youtubers/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteYoutuber(@PathVariable Long id) {
        if (!youtuberService.deleteYoutuber(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "youtuber not found: " + id);
        }
    }

    /**
     * 유튜버 활성/비활성을 토글합니다(자동 배치 크롤링 대상 여부).
     *
     * @param id 토글할 유튜버 PK
     * @return 변경 후 상태 DTO
     */
    @PatchMapping("/youtubers/{id}/toggle")
    public WatchedYoutuberResponse toggleYoutuber(@PathVariable Long id) {
        WatchedYoutuberResponse res = youtuberService.toggleYoutuber(id);
        if (res == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "youtuber not found: " + id);
        }
        return res;
    }

    // ───────────────── 크롤링 트리거·이력 ─────────────────

    /**
     * 채널 전체 숏츠 수를 FastAPI에서 조회합니다.
     * <p>
     * 크롤링 트리거 UI에서 [크롤링] 버튼 클릭 시 호출하여 끝 인덱스 기본값을 자동 채웁니다.
     *
     * @param channelUrl 유튜브 채널 URL
     * @return {@code channel_url}, {@code total_videos} 포함 Map
     */
    @GetMapping("/channel-info")
    public Map<String, Object> getChannelInfo(@RequestParam String channelUrl) {
        return crawlProxyService.getChannelInfo(channelUrl);
    }

    /**
     * FastAPI 데이터 파이프라인에 채널 크롤링을 트리거합니다.
     * <p>
     * ① FastAPI {@code POST /crawl}로 중계 → job_id 획득.<br>
     * ② {@code crawl_history}에 RUNNING 이력 INSERT.
     *
     * @param body 채널 URL·범위·유튜버명
     * @return FastAPI 응답 그대로 — 최소 {@code job_id}, {@code status} 포함
     */
    @PostMapping("/crawl")
    public Map<String, Object> triggerCrawl(@RequestBody CrawlTriggerRequest body) {
        Map<String, Object> result = crawlProxyService.triggerCrawl(body);
        String jobId = result.get("job_id") != null ? result.get("job_id").toString() : null;
        if (jobId != null) {
            youtuberService.saveCrawlHistory(body, jobId);
        }
        return result;
    }

    /**
     * 크롤링 job의 현재 진행 상태를 FastAPI에서 조회해 반환합니다.
     * <p>
     * done/failed 확정 시 {@code crawl_history}를 자동 업데이트하고 유튜버 {@code last_crawled_at}을 갱신합니다.
     *
     * @param jobId {@link #triggerCrawl} 응답의 {@code job_id}
     * @return FastAPI 상태 응답 그대로 — {@code status}, {@code processed}, {@code total}, {@code results} 등 포함
     */
    @GetMapping("/crawl/status/{jobId}")
    public Map<String, Object> getCrawlStatus(@PathVariable String jobId) {
        Map<String, Object> statusMap = crawlProxyService.getCrawlStatus(jobId);
        String status = statusMap.get("status") != null ? statusMap.get("status").toString() : "";
        if ("done".equals(status) || "failed".equals(status)) {
            youtuberService.finishCrawlHistory(jobId, status, statusMap);
        }
        return statusMap;
    }

    /** 크롤링 이력 전체를 최신순으로 반환합니다(어드민 이력 화면). */
    @GetMapping("/crawl/history")
    public List<CrawlHistoryResponse> getCrawlHistory() {
        return youtuberService.listCrawlHistory();
    }

    /**
     * 승인된 (rawName, masterName) 쌍만 일괄 저장합니다. 완료 후 검색 캐시를 재빌드합니다.
     */
    @PostMapping("/ingredients/bulk-map")
    public Map<String, Object> bulkMapIngredients(@RequestBody IngredientBulkMapRequest body) {
        try {
            int updated = adminService.bulkSaveIngredientMappings(body == null ? null : body.getItems());
            return Map.of("updated", updated);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }
}
