package com.yoneodoo.api.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * {@code GET /api/v1/admin/crawl/history} 응답 한 행입니다.
 * {@code resultSummary}는 FastAPI {@code results} 맵을 JSON 문자열로 담은 값입니다.
 */
@Data
@AllArgsConstructor
public class CrawlHistoryResponse {

    private Long id;
    private String youtuberName;
    private String channelUrl;
    private String jobId;
    private Integer startIdx;
    private Integer endIdx;
    /** running / done / failed */
    private String status;
    /** JSON 문자열. 예: {@code {"SUCCESS":3,"NEEDS_REVIEW":1}} */
    private String resultSummary;
    /** manual / batch */
    private String triggeredBy;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
}
