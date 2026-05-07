package com.yoneodoo.api.crawling;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

/**
 * DB 테이블 {@code crawling_data}에 대응하는 엔티티입니다.
 * <p>
 * <b>역할(기획 관점)</b><br>
 * 크롤링 파이프라인에서 수집한 최소 정보(예: 제목)를 임시·로그 성격으로 쌓아 두는 용도로 쓸 수 있습니다.
 * 본 프로젝트의 핵심 레시피 데이터는 {@link com.yoneodoo.api.entity.Recipe} 쪽이 더 중심입니다.
 */
@Entity
@Table(name = "crawling_data")
@Getter
@NoArgsConstructor
public class CrawlingData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 크롤링으로 얻은 제목 등 짧은 텍스트. */
    private String title;

    /** 행이 생성된 시각(DB 기본값으로 채워질 수 있음). */
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
