package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipes")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title; // 레시피 제목

    @Column(name = "youtube_url", nullable = false, length = 500)
    private String youtubeUrl; // 유튜브 Iframe에 쓸 URL

    // 🚀 [추가됨] 파이썬이 파놓은 11자리 영상 ID 칸
    @Column(name = "video_id", unique = true, length = 50)
    private String videoId;

    // 기존: private List<String> ingredients;
    // 변경 후 👇
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<com.yoneodoo.api.dto.RecipeIngredientData> ingredients;

    // 🚀 [추가됨] 데이터의 수집 상태를 기록하는 칸 (SUCCESS, NO_SUBTITLES 등)
    @Column(length = 20)
    private String status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    // 기존 양방향 매핑 유지
    @OneToMany(mappedBy = "recipe", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecipeIngredient> recipeIngredients = new ArrayList<>();

    public Recipe(String title, String youtubeUrl) {
        this.title = title;
        this.youtubeUrl = youtubeUrl;
    }

    // 연관관계 편의 메서드
    public void addRecipeIngredient(RecipeIngredient recipeIngredient) {
        this.recipeIngredients.add(recipeIngredient);
        recipeIngredient.setRecipe(this);
    }
}