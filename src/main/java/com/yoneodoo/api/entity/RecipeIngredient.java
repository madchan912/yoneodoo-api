package com.yoneodoo.api.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Entity
@Table(name = "recipe_ingredients")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", nullable = false)
    private Recipe recipe;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ingredient_id", nullable = false)
    private Ingredient ingredient;

    @Column(precision = 10, scale = 2)
    private BigDecimal amount; // 0.5스푼 같은 소수점 처리

    @Column(length = 50)
    private String unit; // "스푼", "g", "개" 등

    public RecipeIngredient(Ingredient ingredient, BigDecimal amount, String unit) {
        this.ingredient = ingredient;
        this.amount = amount;
        this.unit = unit;
    }

    protected void setRecipe(Recipe recipe) {
        this.recipe = recipe;
    }
}