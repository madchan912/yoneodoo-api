package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * DB 테이블 {@code ingredient_mapping}: "원본 재료명"을 "마스터(표준) 재료명"으로 묶기 위한 사전입니다.
 * <p>
 * <b>왜 필요한가(기획 관점)</b><br>
 * 레시피 JSON에는 같은 재료가 "스팸", "스팸클래식"처럼 다양한 표기로 들어올 수 있습니다.
 * 이 테이블에 {@code raw_name} → {@code master_name} 규칙을 쌓아 두면, 검색·추천·통계에서
 * 사용자에게는 통일된 이름으로 보여 주거나, 내부적으로 같은 재료로 묶어 처리할 수 있습니다.
 * <p>
 * <b>raw_name 저장 규칙</b><br>
 * {@code raw_name}은 레시피 JSON 속 재료명과 <b>동일한 정규화 규칙</b>(앞뒤 공백 제거 + 중간 공백 제거 등,
 * {@link com.yoneodoo.api.admin.IngredientNameNormalizer})으로 맞춰 저장합니다.
 * 그래야 "미분류 목록"과 "매핑 저장" 시 같은 키로 조인·중복 판단이 됩니다.
 * <p>
 * <b>제약</b><br>
 * {@code raw_name}은 유니크: 한 원본 표기는 하나의 마스터만 가집니다.
 */
@Entity
@Table(
        name = "ingredient_mapping",
        uniqueConstraints = @UniqueConstraint(name = "uq_ingredient_mapping_raw_name", columnNames = "raw_name")
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class IngredientMapping {

    /** 매핑 행의 고유 ID(내부 관리용). */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * 레시피 JSON에 등장하는 원본 재료명을 정규화한 값(검색 키 역할).
     * 사용자가 보는 "원문" 그대로가 아니라, 시스템이 맞춰 둔 키 문자열입니다.
     */
    @Column(name = "raw_name", nullable = false, length = 200)
    private String rawName;

    /**
     * 여러 raw를 묶어 대표로 삼을 표준 재료명(예: 모든 스팸류 → "스팸").
     * UI나 검색 결과에서 통일된 라벨로 쓰기 좋습니다.
     */
    @Column(name = "master_name", nullable = false, length = 200)
    private String masterName;

    /** 이 매핑이 처음 저장된 시각(최신순 목록 정렬 등에 사용). */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /** 신규 매핑 한 건을 만들 때 사용하는 편의 생성자입니다. */
    public IngredientMapping(String rawName, String masterName) {
        this.rawName = rawName;
        this.masterName = masterName;
    }
}
