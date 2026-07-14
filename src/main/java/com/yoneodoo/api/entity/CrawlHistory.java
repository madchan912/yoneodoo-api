package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DB 테이블 {@code crawl_history}: 크롤링 job이 실행된 이력을 남깁니다.
 * <p>
 * {@code POST /api/v1/admin/crawl} 호출 시 생성(status=running),
 * {@code GET /api/v1/admin/crawl/status/{jobId}}에서 done/failed 확인 시 업데이트됩니다.
 * {@code result_summary}는 FastAPI 결과({@code results} 맵)를 JSON 문자열로 보관합니다.
 */
@Entity
@Table(name = "crawl_history")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrawlHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 크롤링된 유튜버 표시명. */
    @Column(name = "youtuber_name", length = 100)
    private String youtuberName;

    /** 크롤링된 채널 URL. */
    @Column(name = "channel_url", length = 500)
    private String channelUrl;

    /** FastAPI가 반환한 job UUID. status 폴링·이력 조회 시 기준키. */
    @Column(name = "job_id", length = 100)
    private String jobId;

    /** 수집 시작 인덱스(1-based). 단건 영상 크롤링은 null 가능. */
    @Column(name = "start_idx")
    private Integer startIdx;

    /** 수집 끝 인덱스(inclusive). 단건 영상 크롤링은 null 가능. */
    @Column(name = "end_idx")
    private Integer endIdx;

    /** 실행 상태: running / done / failed. */
    @Column(length = 20)
    private String status;

    /**
     * FastAPI {@code results} 맵을 JSON 문자열로 저장.
     * 예: {@code {"SUCCESS":3,"NEEDS_REVIEW":1,"NO_SUBTITLES":0}}.
     */
    @Column(name = "result_summary", columnDefinition = "TEXT")
    private String resultSummary;

    /** 트리거 방식: manual(어드민 수동) / batch(스케줄러 자동). */
    @Column(name = "triggered_by", length = 20)
    private String triggeredBy;

    /** 크롤링 job이 시작된 시각(트리거 시점). */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 크롤링 job이 완료(done/failed)된 시각. running 상태면 null. */
    @Column(name = "finished_at")
    private LocalDateTime finishedAt;

    public CrawlHistory(String youtuberName, String channelUrl, String jobId,
                        Integer startIdx, Integer endIdx, String triggeredBy) {
        this.youtuberName = youtuberName;
        this.channelUrl = channelUrl;
        this.jobId = jobId;
        this.startIdx = startIdx;
        this.endIdx = endIdx;
        this.triggeredBy = triggeredBy;
        this.status = "running";
        this.startedAt = LocalDateTime.now();
    }
}
