package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter; // 🚀 Setter 추가!
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "recipes")
@Getter
@Setter // 🚀 RecipeService에서 손쉽게 데이터를 세팅할 수 있도록 추가
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String title; // 레시피 제목

    @Column(name = "youtube_url", nullable = false, length = 500)
    private String youtubeUrl; // 유튜브 Iframe에 쓸 URL

    @Column(name = "video_id", unique = true, length = 50)
    private String videoId;

    // 🚀 기존대로 완벽하게 유지! (스프링이 알아서 JSON으로 넣어줍니다)
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<com.yoneodoo.api.dto.RecipeIngredientData> ingredients;

    @Column(length = 20)
    private String status;

    // 🚀 [추가됨] 파이썬이 보내주는 자막 원본 텍스트를 저장할 칸
    @Column(columnDefinition = "TEXT")
    private String transcript;

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

    public void addRecipeIngredient(RecipeIngredient recipeIngredient) {
        this.recipeIngredients.add(recipeIngredient);
        recipeIngredient.setRecipe(this);
    }
}