package com.yoneodoo.api.crawling;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * {@link CrawlingData} 엔티티({@code crawling_data} 테이블)에 대한 기본 CRUD 저장소입니다.
 * 별도 커스텀 쿼리는 없고, Spring Data JPA 기본 메서드({@code findAll} 등)만 사용합니다.
 */
public interface CrawlingDataRepository extends JpaRepository<CrawlingData, Long> {
}
