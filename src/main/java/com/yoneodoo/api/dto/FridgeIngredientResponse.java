package com.yoneodoo.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class FridgeIngredientResponse {
    private Long ingredientId;
    private String name;
    private String type; // MAIN 또는 SUB
}