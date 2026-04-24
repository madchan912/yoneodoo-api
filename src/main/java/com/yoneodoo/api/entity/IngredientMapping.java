package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

/**
 * 크롤링된 원본 재료명(raw)을 마스터 재료명으로 정규화하기 위한 매핑.
 * {@code raw_name}은 레시피 JSON 재료명과 동일한 규칙(공백 제거 등)으로 저장한다.
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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "raw_name", nullable = false, length = 200)
    private String rawName;

    @Column(name = "master_name", nullable = false, length = 200)
    private String masterName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    public IngredientMapping(String rawName, String masterName) {
        this.rawName = rawName;
        this.masterName = masterName;
    }
}
