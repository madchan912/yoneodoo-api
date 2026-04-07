package com.yoneodoo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RecipeIngredientData {
    private String name;    // 예: "고추장"
    private String amount;  // 예: "0.5스푼"
}