package com.yoneodoo.api.admin;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yoneodoo.api.admin.dto.CrawlHistoryResponse;
import com.yoneodoo.api.admin.dto.CrawlTriggerRequest;
import com.yoneodoo.api.admin.dto.WatchedYoutuberRequest;
import com.yoneodoo.api.admin.dto.WatchedYoutuberResponse;
import com.yoneodoo.api.entity.CrawlHistory;
import com.yoneodoo.api.entity.WatchedYoutuber;
import com.yoneodoo.api.repository.CrawlHistoryRepository;
import com.yoneodoo.api.repository.RecipeRepository;
import com.yoneodoo.api.repository.WatchedYoutuberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 유튜버 관리({@code watched_youtubers})·크롤링 이력({@code crawl_history}) 비즈니스 로직입니다.
 * <p>
 * <b>흐름 요약(기획 관점)</b><br>
 * ① 어드민이 유튜버를 등록 → {@code watched_youtubers}에 INSERT.<br>
 * ② 어드민이 크롤링 트리거 → FastAPI 호출 후 이력 RUNNING으로 INSERT.<br>
 * ③ 상태 폴링에서 done/failed 확인 시 이력 UPDATE + 유튜버 {@code last_crawled_at} 갱신.<br>
 * ④ 어드민 이력 화면은 {@code crawl_history}를 최신순으로 반환.
 */
@Service
@RequiredArgsConstructor
public class YoutuberService {

    private final WatchedYoutuberRepository watchedYoutuberRepository;
    private final CrawlHistoryRepository crawlHistoryRepository;
    private final RecipeRepository recipeRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 유튜버 목록을 등록 최신순으로 반환합니다.
     * <p>
     * ① {@code watched_youtubers} 전체 조회.<br>
     * ② 각 유튜버별 레시피 수를 {@code recipes.youtuber_name} 기준으로 실시간 집계해 DTO에 담습니다.
     */
    @Transactional(readOnly = true)
    public List<WatchedYoutuberResponse> listYoutubers() {
        return watchedYoutuberRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(y -> new WatchedYoutuberResponse(
                        y.getId(),
                        y.getChannelUrl(),
                        y.getYoutuberName(),
                        y.isActive(),
                        y.getLastCrawledAt(),
                        recipeRepository.countByYoutuberName(y.getYoutuberName()),
                        y.getCreatedAt()
                ))
                .toList();
    }

    /**
     * 유튜버를 신규 등록합니다.
     * <p>
     * ① 필수 필드 유효성 검사(채널 URL·표시명).<br>
     * ② {@code watched_youtubers}에 INSERT (is_active=true 기본값).
     *
     * @param request 채널 URL·표시명
     * @return 저장된 유튜버 응답 DTO
     */
    @Transactional
    public WatchedYoutuberResponse addYoutuber(WatchedYoutuberRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getChannelUrl())
                || !StringUtils.hasText(request.getYoutuberName())) {
            throw new IllegalArgumentException("channelUrl과 youtuberName은 필수입니다");
        }
        WatchedYoutuber saved = watchedYoutuberRepository.save(
                new WatchedYoutuber(request.getChannelUrl().trim(), request.getYoutuberName().trim())
        );
        return new WatchedYoutuberResponse(
                saved.getId(), saved.getChannelUrl(), saved.getYoutuberName(),
                saved.isActive(), saved.getLastCrawledAt(),
                recipeRepository.countByYoutuberName(saved.getYoutuberName()),
                saved.getCreatedAt()
        );
    }

    /**
     * 유튜버를 목록에서 삭제합니다. 관련 크롤링 이력은 유지됩니다.
     *
     * @param id 삭제할 유튜버 PK
     * @return 대상이 존재해 삭제되면 true, 없으면 false
     */
    @Transactional
    public boolean deleteYoutuber(Long id) {
        if (!watchedYoutuberRepository.existsById(id)) {
            return false;
        }
        watchedYoutuberRepository.deleteById(id);
        return true;
    }

    /**
     * 유튜버의 활성 여부를 토글합니다(자동 배치 크롤링 대상 포함/제외 전환).
     *
     * @param id 토글할 유튜버 PK
     * @return 변경된 상태 DTO. 대상이 없으면 null.
     */
    @Transactional
    public WatchedYoutuberResponse toggleYoutuber(Long id) {
        return watchedYoutuberRepository.findById(id)
                .map(y -> {
                    y.setActive(!y.isActive());
                    WatchedYoutuber saved = watchedYoutuberRepository.save(y);
                    return new WatchedYoutuberResponse(
                            saved.getId(), saved.getChannelUrl(), saved.getYoutuberName(),
                            saved.isActive(), saved.getLastCrawledAt(),
                            recipeRepository.countByYoutuberName(saved.getYoutuberName()),
                            saved.getCreatedAt()
                    );
                })
                .orElse(null);
    }

    /**
     * 크롤링 트리거 직후 이력을 RUNNING 상태로 저장합니다.
     * <p>
     * ① FastAPI가 반환한 job_id와 요청 정보를 함께 INSERT.<br>
     * ② status=running, triggered_by=manual, started_at=현재 시각.
     *
     * @param request 크롤링 트리거 요청 본문(채널 URL·범위·유튜버명)
     * @param jobId   FastAPI가 반환한 job UUID
     */
    @Transactional
    public void saveCrawlHistory(CrawlTriggerRequest request, String jobId) {
        crawlHistoryRepository.save(new CrawlHistory(
                request.getYoutuberName(),
                request.getChannelUrl(),
                jobId,
                request.getStart(),
                request.getEnd(),
                "manual"
        ));
    }

    /**
     * 크롤링 job이 done 또는 failed로 확정되면 이력을 업데이트합니다.
     * <p>
     * ① 이미 종료된 이력은 재갱신하지 않습니다.<br>
     * ② done 시 FastAPI {@code results} 맵을 JSON 문자열로 직렬화해 {@code result_summary}에 저장.<br>
     * ③ done 시 해당 채널의 {@code last_crawled_at}도 갱신합니다.
     *
     * @param jobId     FastAPI job UUID
     * @param newStatus "done" 또는 "failed"
     * @param statusMap FastAPI 상태 응답 전체(results 키 포함)
     */
    @Transactional
    public void finishCrawlHistory(String jobId, String newStatus, Map<String, Object> statusMap) {
        crawlHistoryRepository.findByJobId(jobId).ifPresent(history -> {
            if ("running".equals(history.getStatus())) {
                history.setStatus(newStatus);
                history.setFinishedAt(LocalDateTime.now());
                try {
                    Object results = statusMap.get("results");
                    if (results != null) {
                        history.setResultSummary(objectMapper.writeValueAsString(results));
                    }
                } catch (Exception ignored) {}
                crawlHistoryRepository.save(history);

                // done일 때만 유튜버 last_crawled_at 갱신
                if ("done".equals(newStatus) && history.getChannelUrl() != null) {
                    watchedYoutuberRepository.findByChannelUrl(history.getChannelUrl())
                            .ifPresent(youtuber -> {
                                youtuber.setLastCrawledAt(LocalDateTime.now());
                                watchedYoutuberRepository.save(youtuber);
                            });
                }
            }
        });
    }

    /**
     * 크롤링 이력 전체를 최신순으로 반환합니다(어드민 이력 화면).
     */
    @Transactional(readOnly = true)
    public List<CrawlHistoryResponse> listCrawlHistory() {
        return crawlHistoryRepository.findAllByOrderByStartedAtDesc().stream()
                .map(h -> new CrawlHistoryResponse(
                        h.getId(),
                        h.getYoutuberName(),
                        h.getChannelUrl(),
                        h.getJobId(),
                        h.getStartIdx(),
                        h.getEndIdx(),
                        h.getStatus(),
                        h.getResultSummary(),
                        h.getTriggeredBy(),
                        h.getStartedAt(),
                        h.getFinishedAt()
                ))
                .toList();
    }
}
