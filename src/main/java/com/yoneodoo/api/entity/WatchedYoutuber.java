package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AccessLevel;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * DB 테이블 {@code watched_youtubers}: 크롤링 대상으로 등록된 유튜버 목록입니다.
 * <p>
 * 어드민이 수동으로 등록하며, {@code is_active=true}인 채널만 자동 배치 크롤링 대상이 됩니다.
 * 수동 트리거는 비활성 채널도 가능합니다.
 */
@Entity
@Table(name = "watched_youtubers")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class WatchedYoutuber {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 유튜브 채널 URL. 예: {@code https://www.youtube.com/@유지만} */
    @Column(name = "channel_url", nullable = false, length = 500)
    private String channelUrl;

    /** 관리용 유튜버 표시명(레시피의 youtuber_name 과 일치시키는 것이 권장). */
    @Column(name = "youtuber_name", nullable = false, length = 100)
    private String youtuberName;

    /** 자동 배치 크롤링 포함 여부. false면 배치에서 제외되고 어드민 목록에만 남음. */
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    /** 마지막으로 크롤링이 완료된 시각(done 상태 확정 시 자동 갱신). */
    @Column(name = "last_crawled_at")
    private LocalDateTime lastCrawledAt;

    /** 이 행이 처음 등록된 시각. */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public WatchedYoutuber(String channelUrl, String youtuberName) {
        this.channelUrl = channelUrl;
        this.youtuberName = youtuberName;
    }
}
