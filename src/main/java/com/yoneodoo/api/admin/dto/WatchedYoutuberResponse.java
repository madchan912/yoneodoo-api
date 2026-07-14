package com.yoneodoo.api.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * {@code GET /api/v1/admin/youtubers} 응답 한 행입니다.
 * {@code totalRecipes}는 {@code recipes.youtuber_name} 기준 실시간 집계값입니다.
 */
@Data
@AllArgsConstructor
public class WatchedYoutuberResponse {

    private Long id;
    private String channelUrl;
    private String youtuberName;
    /** true면 자동 배치 크롤링 대상, false면 수동 트리거만 가능. */
    private boolean active;
    /** 마지막 크롤링 완료 시각(done 확정 시 자동 갱신). null이면 아직 크롤링 이력 없음. */
    private LocalDateTime lastCrawledAt;
    /** recipes 테이블에서 집계한 해당 유튜버 레시피 수. */
    private long totalRecipes;
    private LocalDateTime createdAt;
}
