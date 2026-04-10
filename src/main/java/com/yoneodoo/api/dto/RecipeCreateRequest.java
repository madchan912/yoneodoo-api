package com.yoneodoo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RecipeCreateRequest {
    private String videoId;
    private String title;
    private String youtubeUrl;
    private String status;
    private String transcript;
    private String youtuberName;

    // 엔티티에서 사용하는 DTO를 그대로 리스트로 받습니다.
    private List<RecipeIngredientData> ingredients;
}