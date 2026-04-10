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

@Entity
@Table(name = "recipes")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title;

    // 유튜버 이름을 저장할 컬럼을 추가했습니다.
    @Column(name = "youtuber_name", length = 100)
    private String youtuberName;

    @Column(name = "youtube_url", nullable = false, length = 500)
    private String youtubeUrl;

    @Column(name = "video_id", unique = true, length = 50)
    private String videoId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<com.yoneodoo.api.dto.RecipeIngredientData> ingredients;

    @Column(length = 20)
    private String status;

    @Column(columnDefinition = "TEXT")
    private String transcript;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 생성자에서 유튜버 이름을 함께 받을 수 있도록 파라미터를 추가했습니다.
    public Recipe(String title, String youtubeUrl, String youtuberName) {
        this.title = title;
        this.youtubeUrl = youtubeUrl;
        this.youtuberName = youtuberName;
    }
}