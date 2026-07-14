package com.yoneodoo.api.repository;

import com.yoneodoo.api.entity.WatchedYoutuber;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * {@code watched_youtubers} 테이블 접근 계층.
 * <p>
 * 어드민 유튜버 관리 화면({@code GET /api/v1/admin/youtubers})과
 * 자동 배치 크롤링 스케줄러(활성 채널 목록 조회)에서 사용합니다.
 */
public interface WatchedYoutuberRepository extends JpaRepository<WatchedYoutuber, Long> {

    /** 등록 최신순 전체 목록(어드민 목록 화면). */
    List<WatchedYoutuber> findAllByOrderByCreatedAtDesc();

    /** 배치 크롤링 대상: is_active=true인 채널만. */
    List<WatchedYoutuber> findByActiveTrue();

    /** 채널 URL로 단건 조회(lastCrawledAt 갱신 시 사용). */
    Optional<WatchedYoutuber> findByChannelUrl(String channelUrl);
}
