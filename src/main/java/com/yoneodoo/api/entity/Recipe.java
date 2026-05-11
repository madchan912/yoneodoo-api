package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DB 테이블 {@code recipes} 한 행(row)과 1:1로 대응하는 "레시피" 도메인 객체입니다.
 * <p>
 * <b>데이터가 들어오는 경로(기획 관점)</b><br>
 * ① 크롤러(파이썬 등)가 REST API로 레시피 JSON을 보냄 → {@code RecipeController} → {@code RecipeService}가
 * 재료 이름을 정리한 뒤 이 엔티티를 만들어 저장합니다.<br>
 * ② 사용자 앱이 직접 INSERT 하지 않고, 주로 "적재 파이프라인"을 통해 쌓입니다.
 * <p>
 * <b>JSONB 컬럼 {@code ingredients}</b><br>
 * PostgreSQL의 jsonb 타입으로, 한 레시피에 속한 재료 목록(이름·양 등)을 배열 형태로 통째로 보관합니다.
 * 별도의 "재료 테이블"을 두지 않고 레시피 안에 넣는 방식이라, 검색·통계는 서비스 코드에서 JSON을 읽어 처리합니다.
 */
@Entity
@Table(name = "recipes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipe {

    /** DB에서 자동 증가하는 기본키. 다른 테이블이 이 레시피를 참조할 때 사용합니다. */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 요리 제목(검색·목록 표시의 핵심 텍스트). */
    @Column(nullable = false, length = 255)
    private String title;

    /** 영상을 올린 크리에이터 표시명(채널/유튜버 이름 등). */
    @Column(name = "youtuber_name", length = 100)
    private String youtuberName;

    /** 유튜브 영상 전체 URL(원본 링크 보관용). */
    @Column(name = "youtube_url", nullable = false, length = 500)
    private String youtubeUrl;

    /**
     * 유튜브 영상 ID(보통 URL에서 추출). 유니크 제약이 있어 동일 영상이 중복 적재되지 않게 설계할 수 있습니다.
     */
    @Column(name = "video_id", unique = true, length = 50)
    private String videoId;

    /**
     * 재료 목록을 JSON 배열로 저장합니다.
     * <ul>
     *   <li>Java 타입: {@code List<RecipeIngredientData>}</li>
     *   <li>DB 타입: jsonb (Hibernate가 직렬화/역직렬화)</li>
     * </ul>
     * "재료 매핑(정규화)"과 연결될 때는 이 안의 {@code name} 문자열이 {@code ingredient_mapping.raw_name}과
     * 같은 규칙(공백 제거 등)으로 맞춰져야 합니다.
     */
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<com.yoneodoo.api.dto.RecipeIngredientData> ingredients;

    /**
     * 크롤링·자막 처리 등 파이프라인 단계를 나타내는 문자열 코드.
     * 예: 성공({@code SUCCESS}), 자막 없음({@code NO_SUBTITLES}), 대기/미정(null 또는 기타) 등.
     * 어드민 대시보드에서 건수 집계에 사용됩니다.
     * <p>
     * 사용자 노출 여부와는 무관합니다. 노출 토글은 {@link #displayStatus} 컬럼을 사용하세요.
     */
    @Column(length = 20)
    private String status;

    /**
     * 사용자 화면 노출 여부(Soft Delete).
     * 신규 레코드는 {@link DisplayStatus#ACTIVE} 기본값으로 저장되며, 어드민이 {@link DisplayStatus#HIDDEN}으로 토글하면
     * 사용자용 API/검색 캐시에서 제외됩니다.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "display_status", nullable = false, length = 20,
            columnDefinition = "varchar(20) NOT NULL DEFAULT 'ACTIVE'")
    private DisplayStatus displayStatus = DisplayStatus.ACTIVE;

    /** 자막·스크립트 원문(긴 텍스트). 검색이나 요약 기능에 활용할 수 있는 원천 데이터입니다. */
    @Column(columnDefinition = "TEXT")
    private String transcript;

    /** 이 행이 DB에 처음 저장된 시각(자동 기록, 이후 수정되지 않음). */
    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    /**
     * 최소 정보로 엔티티를 만들 때 사용하는 생성자입니다.
     * 나머지 필드({@code videoId}, {@code status}, {@code ingredients} 등)는 서비스에서 setter로 채웁니다.
     */
    public Recipe(String title, String youtubeUrl, String youtuberName) {
        this.title = title;
        this.youtubeUrl = youtubeUrl;
        this.youtuberName = youtuberName;
    }
}
