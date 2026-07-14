package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.CrawlHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@code crawl_history} 테이블 접근 계층.
 * <p>
 * 크롤링 트리거 시 INSERT, 상태 폴링에서 done/failed 확인 시 UPDATE,
 * 어드민 이력 화면({@code GET /api/v1/admin/crawl/history})에서 SELECT합니다.
 */
public interface CrawlHistoryRepository extends JpaRepository<CrawlHistory, Long> {

    /** job_id로 이력 단건 조회(상태 업데이트용). */
    Optional<CrawlHistory> findByJobId(String jobId);

    /** 시작 시각 최신순 전체 이력(어드민 이력 화면). */
    List<CrawlHistory> findAllByOrderByStartedAtDesc();
}
